package com.jjg.game.activity.privilegecard.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.privilegecard.data.PlayerPrivilegeCard;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardDetailInfo;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardType;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardClaimRewards;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardDetailInfo;
import com.jjg.game.activity.privilegecard.message.res.ResPrivilegeCardTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.PrivilegeCardCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PrivilegeCardController
 * <p>
 * 管理“特权卡”活动相关逻辑，包括：
 * 1. 玩家加入特权卡活动
 * 2. 玩家每日领取奖励
 * 3. 活动详情查询与构建
 * 4. 活动状态构建
 *
 * @author lm
 * @date 2025/9/3
 */
@Component
public class PrivilegeCardController extends BaseActivityController implements GameEventListener, OrderGenerate {

    private final Logger log = LoggerFactory.getLogger(PrivilegeCardController.class);

    /**
     * 获取玩家特权卡的领取状态
     *
     * @param data       玩家特权卡数据
     * @param timeMillis 当前时间戳
     * @return 领取状态（未领取/已领取/可领取）
     */
    public int getClaimStatus(PlayerPrivilegeCard data, long timeMillis) {
        // 如果特权卡已过期，则不可领取
        if (data.getEndTime() != -1 && data.getEndTime() < timeMillis) {
            return ActivityConstant.ClaimStatus.NOT_CLAIM;
        }
        // 如果今天已领取，则状态为已领取
        if (TimeHelper.inSameDay(data.getLastClaimTime(), timeMillis)) {
            return ActivityConstant.ClaimStatus.CLAIMED;
        }
        // 其他情况可领取
        return ActivityConstant.ClaimStatus.CAN_CLAIM;
    }

    @Override
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis();
        for (PlayerPrivilegeCard data : playerActivityData.values()) {
            if (getClaimStatus(data, currentTimeMillis) == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                return true;
            }
        }
        return false;
    }

    /**
     * 玩家加入特权卡活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        加入次数（暂未使用）
     * @return 返回玩家特权卡活动详情
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResPrivilegeCardDetailInfo res = null;
        long playerId = player.getId();
        Map<Integer, PrivilegeCardCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        PrivilegeCardCfg cfg = baseCfgBeanMap.get(detailId);

        if (cfg != null) {
            LocalDateTime nowMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
            long timeMillis = TimeHelper.getTimestamp(nowMidnight);
            PlayerPrivilegeCard privilegeCard = null;
            CommonResult<ItemOperationResult> addedItems = null;
            String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());

            // 加锁，防止并发修改
            boolean lock = false;
            try {
                lock = redisLock.tryLockWithDefaultTime(lockKey);
                if (!lock) {
                    log.error("获取锁失败 lockKey:{} playerId:{} activityId:{} detailId:{} times:{}", lockKey, playerId, activityData.getId(), detailId, times);
                    return res;
                }
                Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
                // 获取玩家特权卡数据，若不存在则创建
                privilegeCard = playerActivityData.computeIfAbsent(detailId, key -> new PlayerPrivilegeCard(activityData.getId(), activityData.getRound()));

                // 判断玩家是否已购买特权卡
                if (privilegeCard.getEndTime() > timeMillis) {
                    log.error("玩家已参加活动 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    return res;
                }

                // 设置购买时间
                privilegeCard.setBuyTime(timeMillis);

                // 设置结束时间
                if (cfg.getDays() == -1) {
                    privilegeCard.setEndTime(-1); // 永久有效
                } else {
                    privilegeCard.setEndTime(TimeHelper.getTimestamp(nowMidnight.plusDays(cfg.getDays())));
                }

                // 购买奖励发放
                if (CollectionUtil.isNotEmpty(cfg.getGetItem())) {
                    addedItems = playerPackService.addItems(playerId, cfg.getGetItem(), AddType.ACTIVITY_PRIVILEGE_CARD_BUY);
                    if (!addedItems.success()) {
                        log.error("发放购买奖励失败 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                    }
                }

                // 保存玩家活动数据
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);

            } catch (Exception e) {
                log.error("玩家加入特权卡活动异常 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId, e);
            } finally {
                if (lock) {
                    redisLock.tryUnlock(lockKey);
                }
            }

            // 发送日志
            activityLogger.sendPrivilegeCardJoinLog(player, activityData, cfg, addedItems == null ? null : addedItems.data, cfg.getGetItem());

            // 构建响应数据
            res = new ResPrivilegeCardDetailInfo(Code.SUCCESS);
            res.detailInfo = new ArrayList<>();
            res.detailInfo.add(buildPlayerActivityDetail(player, activityData, cfg, privilegeCard));

        } else {
            log.error("活动配置为空 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
        }

        return res;
    }

    /**
     * 玩家每日领取特权卡奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @return 返回领取奖励结果
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResPrivilegeCardClaimRewards res = new ResPrivilegeCardClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        Map<Integer, PrivilegeCardCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        PrivilegeCardCfg cfg = baseCfgBeanMap.get(detailId);
        if (cfg == null || CollectionUtil.isEmpty(cfg.getDayRebate())) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        PlayerPrivilegeCard data = null;
        CommonResult<ItemOperationResult> addedItems = null;
        // 加锁，保证领取操作原子性
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(lockKey);
            if (!lock) {
                res.code = Code.FAIL;
                log.error("获取锁失败 lockKey:{} playerId:{} activityId:{} detailId:{} ", lockKey, playerId, activityData.getId(), detailId);
                return res;
            }
            Map<Integer, PlayerPrivilegeCard> dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            if (CollectionUtil.isEmpty(dataMap)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }

            data = dataMap.get(detailId);
            if (data == null) {
                res.code = Code.PARAM_ERROR;
                return res;
            }

            long timeMillis = System.currentTimeMillis();
            int claimStatus = getClaimStatus(data, timeMillis);
            if (claimStatus != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                res.code = Code.REPEAT_OP;
                return res;
            }

            // 发放每日奖励
            addedItems = playerPackService.addItems(playerId, cfg.getDayRebate(), AddType.ACTIVITY_PRIVILEGE_REWARDS);
            if (!addedItems.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }

            // 更新领取时间
            data.setLastClaimTime(timeMillis);
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);

        } catch (Exception e) {
            log.error("领取每日奖励异常 playerId:{} activityId:{} detailid:{}", playerId, activityData.getId(), detailId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(lockKey);
            }
        }

        // 构建响应数据
        if (data != null && addedItems != null && addedItems.success()) {
            //计算天数
            long remain = data.getEndTime() == -1 ? data.getEndTime() : ChronoUnit.DAYS.between(LocalDateTime.now(), TimeHelper.getLocalDateTime(data.getEndTime()));
            activityLogger.sendPrivilegeCardRewardsLog(player, activityData, cfg, remain, addedItems.data, cfg.getDayRebate());
        }
        res.activityId = activityData.getId();
        res.detailId = detailId;
        res.infoList = ItemUtils.buildItemInfo(cfg.getDayRebate());
        res.detailInfo = buildPlayerActivityDetail(player, activityData, cfg, data);
        return res;
    }

    /**
     * 构建玩家特权卡活动详情
     *
     * @param player
     * @param activityData 活动ID
     * @param baseCfgBean  活动配置
     * @param data         玩家特权卡数据
     * @return 返回特权卡详情信息
     */
    @Override
    public PrivilegeCardDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof PrivilegeCardCfg cfg)) {
            return null;
        }

        PrivilegeCardDetailInfo info = new PrivilegeCardDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = baseCfgBean.getId();
        info.rechargePrice = cfg.getPurchasecost().toPlainString();

        // 合并总奖励（购买奖励 + 累计返利）
        Map<Integer, Long> totalGetHashMap = new HashMap<>(cfg.getTotalRebate());
        totalGetHashMap.putAll(cfg.getGetItem());
        info.totalGet = ItemUtils.buildItemInfo(totalGetHashMap);

        // 当日奖励信息
        info.rewardItems = ItemUtils.buildItemInfo(cfg.getDayRebate());
        info.days = cfg.getDays();
        //商品id
        if (CollectionUtil.isNotEmpty(cfg.getChannelCommodity())) {
            info.productId = cfg.getChannelCommodity().get(player.getChannel().getValue());
        }
        long timeMillis = System.currentTimeMillis();
        if (data instanceof PlayerPrivilegeCard privilegeCard) {
            info.claimStatus = getClaimStatus(privilegeCard, timeMillis);
            info.remainTime = privilegeCard.getEndTime() - timeMillis;
        }

        return info;
    }

    /**
     * 获取玩家特权卡活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResPrivilegeCardDetailInfo detailInfo = new ResPrivilegeCardDetailInfo(Code.SUCCESS);

        Map<Integer, PrivilegeCardCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        Map<Integer, PlayerPrivilegeCard> playerActivityData = playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityId);

        detailInfo.detailInfo = new ArrayList<>();
        BaseActivityDetailInfo baseActivityDetailInfo = buildPlayerActivityDetail(player, activityData, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId));
        if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo cardDetailInfo) {
            detailInfo.detailInfo.add(cardDetailInfo);
        }

        return detailInfo;
    }

    /**
     * 构建活动类型信息
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResPrivilegeCardTypeInfo cardTypeInfo = new ResPrivilegeCardTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }

        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            PrivilegeCardType privilegeCardType = new PrivilegeCardType();
            privilegeCardType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(privilegeCardType);

            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof PrivilegeCardDetailInfo info) {
                    privilegeCardType.detailInfos.add(info);
                }
            }
        }

        return cardTypeInfo;
    }


    @Override
    public Map<Integer, PrivilegeCardCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getPrivilegeCardCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            Order order = event.getOrder();
            Player player = event.getPlayer();
            if (order.getRechargeType() != getRechargeType()) {
                return;
            }
            dealActivityRecharge(player, order, 1);
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        BaseCfgBean cfgBean = getOrderGenerateBean(player, req.productId);
        if (cfgBean instanceof PrivilegeCardCfg cfg) {
            String channelCommodity = cfg.getChannelCommodity().get(player.getChannel().getValue());
            if (channelCommodity == null) {
                return null;
            }
            return cfg.getPurchasecost();
        }
        return null;
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.PRIVILEGE_CARD;
    }
}
