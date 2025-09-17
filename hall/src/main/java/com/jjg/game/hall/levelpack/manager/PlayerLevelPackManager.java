package com.jjg.game.hall.levelpack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.sharepromote.dao.PlayerLevelDao;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.levelpack.message.bean.PlayerLevelPackDetailInfo;
import com.jjg.game.hall.levelpack.message.data.PlayerLevelPackData;
import com.jjg.game.hall.levelpack.message.req.ReqPlayerLevelClaimRewards;
import com.jjg.game.hall.levelpack.message.res.NotifyPlayerLevelPackDetailInfo;
import com.jjg.game.hall.levelpack.message.res.ResPlayerLevelClaimRewards;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PlayerLevelPackCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class PlayerLevelPackManager implements GameEventListener {
    private final Logger log = LoggerFactory.getLogger(PlayerLevelPackManager.class);

    private final RedisLock redisLock;
    private final PlayerLevelDao playerLevelDao;
    private final ClusterSystem clusterSystem;
    private final PlayerPackService playerPackService;
    //redis持有锁时间
    private final int REDIS_LOCK_TIME = 200;

    public PlayerLevelPackManager(RedisLock redisLock, PlayerLevelDao playerLevelDao, ClusterSystem clusterSystem, PlayerPackService playerPackService) {
        this.redisLock = redisLock;
        this.playerLevelDao = playerLevelDao;
        this.clusterSystem = clusterSystem;
        this.playerPackService = playerPackService;
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
        redisLock.lock(lockKey, REDIS_LOCK_TIME);
        try {
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
            redisLock.unlock(lockKey);
        }
        if (change) {
            NotifyPlayerLevelPackDetailInfo info = buildNotifyPlayerLevelPackDetailInfo(playerLevelPackData);
            clusterSystem.sendToPlayer(info, playerId);
        }
    }


    public NotifyPlayerLevelPackDetailInfo buildNotifyPlayerLevelPackDetailInfo(Map<Integer, PlayerLevelPackData> playerLevelPackData) {
        NotifyPlayerLevelPackDetailInfo info = new NotifyPlayerLevelPackDetailInfo();
        if (CollectionUtil.isEmpty(playerLevelPackData)) {
            return info;
        }
        long currentTimeMillis = System.currentTimeMillis();
        info.detailInfo = new ArrayList<>(playerLevelPackData.size());
        for (PlayerLevelPackData data : playerLevelPackData.values()) {
            if (data.getClaimStatus() == HallConstant.ClaimStatus.CLAIMED) {
                continue;
            }
            PlayerLevelPackCfg packCfg = GameDataManager.getPlayerLevelPackCfg(data.getId());
            if (packCfg == null) {
                continue;
            }
            PlayerLevelPackDetailInfo detailInfo = new PlayerLevelPackDetailInfo();
            detailInfo.buyPrice = packCfg.getPay();
            detailInfo.claimStatus = data.getClaimStatus();
            detailInfo.remainTime = data.getBuyEndTime() - currentTimeMillis;
            detailInfo.rewardItems = ItemUtils.buildItemInfo(packCfg.getLevelRewards());
            detailInfo.id = data.getId();
            info.detailInfo.add(detailInfo);
        }
        return info;
    }


    public AbstractResponse ReqPlayerLevelClaimRewards(PlayerController playerController, ReqPlayerLevelClaimRewards req) {
        ResPlayerLevelClaimRewards res = new ResPlayerLevelClaimRewards(Code.SUCCESS);
        PlayerLevelPackCfg packCfg = GameDataManager.getPlayerLevelPackCfg(req.id);
        if (packCfg == null || CollectionUtil.isEmpty(packCfg.getLevelRewards())) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        long playerId = playerController.playerId();
        PlayerLevelPackData playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId, req.id);
        if (playerLevelPackData == null || playerLevelPackData.getClaimStatus() == HallConstant.ClaimStatus.NOT_CLAIM) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (playerLevelPackData.getClaimStatus() == HallConstant.ClaimStatus.CLAIMED) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        String lockKey = playerLevelDao.getLockKey(playerId);
        redisLock.lock(lockKey, REDIS_LOCK_TIME);
        CommonResult<ItemOperationResult> added = null;
        try {
            playerLevelPackData = playerLevelDao.getPlayerLevelPackData(playerId, req.id);
            //领取奖励
            if (playerLevelPackData != null && playerLevelPackData.getClaimStatus() == HallConstant.ClaimStatus.CAN_CLAIM) {
                added = playerPackService.addItems(playerId, packCfg.getLevelRewards(), "playerLevelClaim");
                if (!added.success()) {
                    log.error("等级礼包添加道具失败 playerId:{} id:{} ", playerId, req.id);
                }
                playerLevelPackData.setClaimStatus(HallConstant.ClaimStatus.CLAIMED);
                playerLevelDao.savePackData(playerId, req.id, playerLevelPackData);
            }
        } catch (Exception e) {
            log.error("等级礼包 领取奖励异常 playerId:{} id:{} ", playerId, req.id, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        if (added != null && added.success()) {
            res.itemInfos = ItemUtils.buildItemInfo(packCfg.getLevelRewards());
        }
        return res;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEvent event) {
            targetGift(event.getPlayer());
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.PLAYER_LEVEL);
    }
}
