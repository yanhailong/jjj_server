package com.jjg.game.activity.levelpack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.activitylog.ActivityLogger;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.levelpack.dao.PlayerLevelDao;
import com.jjg.game.activity.levelpack.data.PlayerLevelPackData;
import com.jjg.game.activity.levelpack.message.bean.PlayerLevelPackDetailInfo;
import com.jjg.game.activity.levelpack.message.req.ReqPlayerLevelClaimRewards;
import com.jjg.game.activity.levelpack.message.res.NotifyPlayerLevelPackDetailInfo;
import com.jjg.game.activity.levelpack.message.res.ResPlayerLevelClaimRewards;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.NotifyPlayerLevelUp;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PlayerLevelConfigCfg;
import com.jjg.game.sampledata.bean.PlayerLevelPackCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class PlayerLevelPackManager implements GameEventListener, OrderGenerate, IRedDotService {
    private final Logger log = LoggerFactory.getLogger(PlayerLevelPackManager.class);

    private final RedisLock redisLock;
    private final PlayerLevelDao playerLevelDao;
    private final ClusterSystem clusterSystem;
    private final PlayerPackService playerPackService;
    private final ActivityLogger activityLogger;
    private final CoreSendMessageManager coreSendMessageManager;
    private final RedDotManager redDotManager;
    private final GameFunctionService gameFunctionService;

    public PlayerLevelPackManager(RedisLock redisLock, PlayerLevelDao playerLevelDao, ClusterSystem clusterSystem,
                                  PlayerPackService playerPackService, ActivityLogger activityLogger, CoreSendMessageManager coreSendMessageManager, RedDotManager redDotManager, GameFunctionService gameFunctionService) {
        this.redisLock = redisLock;
        this.playerLevelDao = playerLevelDao;
        this.clusterSystem = clusterSystem;
        this.playerPackService = playerPackService;
        this.activityLogger = activityLogger;
        this.coreSendMessageManager = coreSendMessageManager;
        this.redDotManager = redDotManager;
        this.gameFunctionService = gameFunctionService;
    }

    /**
     * 玩家参与等级礼包
     *
     * @param player 玩家对象
     */
    public void targetGift(Player player) {
        if (player == null) {
            log.error("玩家等级变化时触发等级礼包为null");
            return;
        }
        if (!gameFunctionService.checkGameFunctionOpen(player, EFunctionType.LEVEL_GIFT, true, false)) {
            log.info("玩家等级变化时触发等级礼包 未满足开启条件");
            return;
        }
        long playerLevel = player.getLevel();
        long playerId = player.getId();
        List<PlayerLevelPackCfg> playerLevelPackCfgList = GameDataManager.getPlayerLevelPackCfgList();
        Map<Integer, PlayerLevelPackData> playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId);
        List<PlayerLevelPackCfg> playerLevelPack = new ArrayList<>(playerLevelPackData.size());
        for (PlayerLevelPackCfg cfgBean : playerLevelPackCfgList) {
            //当等级比配置大且不包含在活动数据里面的添加
            if (cfgBean instanceof PlayerLevelPackCfg cfg && playerLevelPackData.get(cfg.getId()) == null && playerLevel >= cfg.getPlayerlevel()) {
                playerLevelPack.add(cfg);
            }
        }
        if (playerLevelPack.isEmpty()) {
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        boolean change = false;
        String lockKey = playerLevelDao.getLockKey(playerId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(lockKey);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} playerId:{} ", lockKey, playerId);
                return;
            }
            //双重校验
            playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId);
            for (PlayerLevelPackCfg packCfg : playerLevelPack) {
                PlayerLevelPackData packData = playerLevelPackData.get(packCfg.getId());
                if (packData != null) {
                    continue;
                }
                //构建新的PlayerLevelPackData
                PlayerLevelPackData data = new PlayerLevelPackData();
                data.setTargetTime(currentTimeMillis);
                data.setId(packCfg.getId());
                data.setClaimStatus(ActivityConstant.ClaimStatus.NOT_CLAIM);
                data.setBuyEndTime((long) packCfg.getTime() * TimeHelper.ONE_MINUTE_OF_MILLIS + currentTimeMillis);
                playerLevelPackData.put(packCfg.getId(), data);
                change = true;
            }
            //回存等级礼包数据
            if (change) {
                playerLevelDao.saveAllPackData(playerId, playerLevelPackData);
            }
        } catch (Exception e) {
            log.error("等级变化时修改玩家活动数据失败 playerId:{} playerLevel:{} ", playerId, playerLevel, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(lockKey);
            }
        }
        if (change) {
            NotifyPlayerLevelPackDetailInfo info = buildNotifyPlayerLevelPackDetailInfo(player, playerLevelPackData);
            clusterSystem.sendToPlayer(info, playerId);
            updateRedDot(player.getId(), false);
        }
    }


    public NotifyPlayerLevelPackDetailInfo buildNotifyPlayerLevelPackDetailInfo(Player player, Map<Integer, PlayerLevelPackData> playerLevelPackData) {
        NotifyPlayerLevelPackDetailInfo info = new NotifyPlayerLevelPackDetailInfo();
        if (CollectionUtil.isEmpty(playerLevelPackData)) {
            return info;
        }
        long currentTimeMillis = System.currentTimeMillis();
        info.detailInfo = new ArrayList<>(playerLevelPackData.size());
        for (PlayerLevelPackData data : playerLevelPackData.values()) {
            if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED || data.getBuyEndTime() < currentTimeMillis) {
                continue;
            }
            PlayerLevelPackCfg packCfg = GameDataManager.getPlayerLevelPackCfg(data.getId());
            if (packCfg == null) {
                continue;
            }
            PlayerLevelPackDetailInfo detailInfo = new PlayerLevelPackDetailInfo();
            detailInfo.buyPrice = packCfg.getPay().toPlainString();
            detailInfo.claimStatus = data.getClaimStatus();
            detailInfo.level = packCfg.getPlayerlevel();
            detailInfo.remainTime = data.getBuyEndTime() - currentTimeMillis;
            detailInfo.rewardItems = ItemUtils.buildItemInfo(packCfg.getLevelRewards());
            detailInfo.id = data.getId();
            //商品id
            if (CollectionUtil.isNotEmpty(packCfg.getChannelCommodity())) {
                detailInfo.productId = packCfg.getChannelCommodity().get(player.getChannel().getValue());
            }
            info.detailInfo.add(detailInfo);
        }
        return info;
    }


    public AbstractResponse reqPlayerLevelClaimRewards(PlayerController playerController, ReqPlayerLevelClaimRewards req) {
        ResPlayerLevelClaimRewards res = new ResPlayerLevelClaimRewards(Code.SUCCESS);
        PlayerLevelPackCfg packCfg = GameDataManager.getPlayerLevelPackCfg(req.id);
        //检查配置
        if (packCfg == null || CollectionUtil.isEmpty(packCfg.getLevelRewards())) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        long playerId = playerController.playerId();
        PlayerLevelPackData playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId, req.id);
        //检查等级礼包数据
        if (playerLevelPackData == null || playerLevelPackData.getClaimStatus() == ActivityConstant.ClaimStatus.NOT_CLAIM) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        //检查领奖状态
        if (playerLevelPackData.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        String lockKey = playerLevelDao.getLockKey(playerId);
        CommonResult<ItemOperationResult> added = null;
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(lockKey);
            if (!lock) {
                log.error("获取锁失败 lockKey:{} playerId:{} ", lockKey, playerId);
                res.code = Code.FAIL;
                return res;
            }
            playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId, req.id);
            //领取奖励
            if (playerLevelPackData != null && playerLevelPackData.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                added = playerPackService.addItems(playerId, packCfg.getLevelRewards(), AddType.LEVEL_CLAIM);
                if (!added.success()) {
                    log.error("等级礼包添加道具失败 playerId:{} id:{} ", playerId, req.id);
                }
                playerLevelPackData.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerLevelDao.savePackData(playerId, req.id, playerLevelPackData);
            }
        } catch (Exception e) {
            log.error("等级礼包 领取奖励异常 playerId:{} id:{} ", playerId, req.id, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(lockKey);
            }
        }
        if (added != null && added.success()) {
            res.itemInfos = ItemUtils.buildItemInfo(packCfg.getLevelRewards());
            activityLogger.sendLevelPackClaimLog(playerController.getPlayer(), added.data, packCfg);
        }
        return res;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEvent event) {
            if (event.getGameEventType() == EGameEventType.PLAYER_LEVEL) {
                targetGift(event.getPlayer());
                if (event.getNewlyValue() instanceof Integer newLevel &&
                        event.getEventChangeValue() instanceof Integer oldLevel) {
                    levelUp(event.getPlayer(), oldLevel, newLevel);
                }
            }
        }

    }

    /**
     * 处理玩家充值礼包
     *
     * @param player 玩家信息
     */
    private boolean dealRecharge(Player player, Order order) {
        long playerId = player.getId();
        PlayerLevelPackCfg playerLevelPackCfg;
        CommonResult<ItemOperationResult> added;
        try {
            String productId = order.getProductId();
            int id = Integer.parseInt(productId);
            playerLevelPackCfg = GameDataManager.getPlayerLevelPackCfg(id);
            if (playerLevelPackCfg == null) {
                log.error("玩家购买等级礼包失败 配置不存在 playerId:{} order:{}", playerId, JSONObject.toJSONString(order));
                return false;
            }
            String lockKey = playerLevelDao.getLockKey(playerId);
            Map<Integer, PlayerLevelPackData> playerLevelPackDataMap;
            PlayerLevelPackData playerLevelPackData;
            boolean lock = false;
            try {
                lock = redisLock.tryLockWithDefaultTime(lockKey);
                if (!lock) {
                    log.error("获取锁失败 lockKey:{} playerId:{} orderId:{}", lockKey, playerId, order.getId());
                    return false;
                }
                playerLevelPackDataMap = playerLevelDao.getPlayerLevelPackData(playerId);
                if (CollectionUtil.isEmpty(playerLevelPackDataMap)) {
                    log.error("玩家购买等级礼包失败 没有任何等级礼包数据 playerId:{} id:{}", playerId, id);
                    return false;
                }
                playerLevelPackData = playerLevelPackDataMap.get(id);
                //购买条件判断
                if (playerLevelPackData == null || playerLevelPackData.getClaimStatus() != ActivityConstant.ClaimStatus.NOT_CLAIM) {
                    log.error("玩家购买等级礼包失败 数据不存在 playerLevelPackData:{} playerId:{} id:{}", playerLevelPackData, playerId, id);
                    return false;
                }
                added = playerPackService.addItems(playerId, playerLevelPackCfg.getLevelRewards(), AddType.LEVEL_CLAIM);
                if (!added.success()) {
                    log.error("等级礼包领取道具失败 playerId:{} id:{} code:{}", playerId, id, added.code);
                    return false;
                }
                playerLevelPackData.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerLevelDao.savePackData(playerId, id, playerLevelPackData);
            } finally {
                if (lock) {
                    redisLock.tryUnlock(lockKey);
                }
            }
            activityLogger.sendLevelPackClaimLog(player, added.data, playerLevelPackCfg);
            activityLogger.sendLevelPackBuyLog(player, playerLevelPackCfg);
            ResPlayerLevelClaimRewards res = new ResPlayerLevelClaimRewards(Code.SUCCESS);
            res.id = id;
            res.itemInfos = ItemUtils.buildItemInfo(playerLevelPackCfg.getLevelRewards());
            try {
                clusterSystem.sendToPlayer(res, playerId);
            } catch (Exception e) {
                log.error("等级礼包充值后通知玩家异常 playerId:{} order:{} ", playerId, JSONObject.toJSONString(order), e);
            }
            return true;
        } catch (Exception e) {
            log.error("等级礼包购买异常 playerId:{} order:{}", playerId, JSONObject.toJSONString(order), e);
            return false;
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.PLAYER_LEVEL, EGameEventType.RECHARGE);
    }


    public AbstractResponse reqPlayerLevelPackDetailInfo(PlayerController playerController) {
        Map<Integer, PlayerLevelPackData> playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerController.playerId());
        return buildNotifyPlayerLevelPackDetailInfo(playerController.getPlayer(), playerLevelPackData);
    }

    /**
     * 玩家升级会赠送道具
     *
     * @param player 玩家数据
     */
    private void levelUp(Player player, int oldLevel, int newLevel) {
        Map<Integer, Long> addItemsMap = new HashMap<>();
        for (int i = oldLevel + 1; i <= newLevel; i++) {
            //获取配置
            PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(i);
            if (playerLevelConfigCfg == null) {
                continue;
            }
            //检查是否有道具配置
            if (CollectionUtil.isEmpty(playerLevelConfigCfg.getGetItem())) {
                continue;
            }
            playerLevelConfigCfg.getGetItem().forEach((key, value) -> {
                addItemsMap.merge(key, value, Long::sum);
            });
        }

        List<ItemInfo> items = null;
        CommonResult<ItemOperationResult> result = null;
        if (!addItemsMap.isEmpty()) {
            //添加道具
            result = playerPackService.addItems(player.getId(), addItemsMap, AddType.LEVEL_UPGRADE);
            if (!result.success()) {
                log.warn("玩家升级添加道具失败 playerId = {},level = {},code = {}", player.getId(), newLevel, result.code);
            } else {
                NotifyPlayerLevelUp notify = new NotifyPlayerLevelUp();
                notify.level = newLevel;

                items = new ArrayList<>();

                for (Map.Entry<Integer, Long> en : addItemsMap.entrySet()) {
                    ItemInfo itemInfo = new ItemInfo();

                    itemInfo.itemId = en.getKey();
                    itemInfo.count = en.getValue();

                    items.add(itemInfo);
                }
                notify.items = items;

                clusterSystem.sendToPlayer(notify, player.getId());
            }
        }

        coreSendMessageManager.buildBaseInfoChangeMessage(player);
        activityLogger.level(player, oldLevel, newLevel, items, result
        );
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        int id = Integer.parseInt(req.productId);
        PlayerLevelPackCfg cfg = GameDataManager.getPlayerLevelPackCfg(id);
        if (cfg == null) {
            return null;
        }
        String channelCommodity = cfg.getChannelCommodity().get(player.getChannel().getValue());
        if (channelCommodity == null) {
            return null;
        }
        return cfg.getPay();
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.PLAYER_LEVEL_GIFT;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() == getRechargeType()) {
            return dealRecharge(player, order);
        }
        return true;
    }

    /**
     * 更新红点
     */
    public void updateRedDot(long playerId, boolean oldState) {
        boolean hasRed = hasRedDot(playerId);
        if (oldState != hasRed) {
            List<RedDotDetails> list = new ArrayList<>();
            RedDotDetails redDotDetailInfo = new RedDotDetails();
            redDotDetailInfo.setRedDotModule(RedDotDetails.RedDotModule.LEVEL_PACK);
            redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COMMON);
            redDotDetailInfo.setCount(hasRed ? 1 : 0);
            list.add(redDotDetailInfo);
            redDotManager.updateRedDot(list, playerId);
        }
    }

    /**
     * 是否有红点
     */
    private boolean hasRedDot(long playerId) {
        Map<Integer, PlayerLevelPackData> playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId);
        if (CollectionUtil.isEmpty(playerLevelPackData)) {
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis();
        for (PlayerLevelPackData packData : playerLevelPackData.values()) {
            if (packData.getBuyEndTime() >= currentTimeMillis || packData.getClaimStatus() == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.LEVEL_PACK;
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        RedDotDetails redDotDetailInfo = new RedDotDetails();
        redDotDetailInfo.setRedDotModule(RedDotDetails.RedDotModule.LEVEL_PACK);
        redDotDetailInfo.setRedDotType(RedDotDetails.RedDotType.COMMON);
        redDotDetailInfo.setCount(hasRedDot(playerId) ? 1 : 0);
        return List.of(redDotDetailInfo);
    }
}
