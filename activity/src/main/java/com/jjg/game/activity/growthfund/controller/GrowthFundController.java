package com.jjg.game.activity.growthfund.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.ClaimRewardsResult;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.growthfund.message.bean.GrowthFundActivityInfo;
import com.jjg.game.activity.growthfund.message.bean.GrowthFundDetailInfo;
import com.jjg.game.activity.growthfund.message.res.ResGrowthFundBuyResultInfo;
import com.jjg.game.activity.growthfund.message.res.ResGrowthFundClaimRewards;
import com.jjg.game.activity.growthfund.message.res.ResGrowthFundDetailInfo;
import com.jjg.game.activity.growthfund.message.res.ResGrowthFundTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.GrowthFundCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class GrowthFundController extends BaseActivityController implements GameEventListener {

    private final Logger log = LoggerFactory.getLogger(GrowthFundController.class);
    private final CountDao countDao;

    public GrowthFundController(CountDao countDao) {
        this.countDao = countDao;
    }

    /**
     * 玩家加入成长基金活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        加入次数（暂未使用）
     * @return 返回玩家特权卡活动详情
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        long playerId = player.getId();
        long activityId = activityData.getId();
        BigDecimal decimal = countDao.incr(String.valueOf(activityId), String.valueOf(playerId));
        if (decimal.intValue() >= 1) {
            log.error("玩家已经购买过成长基金 playerId:{} activityId:{}", playerId, activityId);
            return null;
        }
        //获取最新的玩家等级数据
        Player newPlayer = corePlayerService.get(playerId);
        int level = newPlayer.getLevel();
        //获取配置信息
        Map<Integer, GrowthFundCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        //需要更新的详情信息
        Set<GrowthFundCfg> updateDetailId = new HashSet<>();
        Map<Integer, PlayerActivityData> playerActivityData = null;
        String lockKey = playerActivityDao.getLockKey(playerId, activityId);
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            //触发需要购买的奖励
            for (GrowthFundCfg cfg : baseCfgBeanMap.values()) {
                //等级不够 和 不需要支付的跳过
                if (cfg.getLevel() > level && cfg.getType() == ActivityConstant.GrowthFund.Charge) {
                    continue;
                }
                //判断是否能触发
                PlayerActivityData data = playerActivityData.computeIfAbsent(cfg.getId(), key -> new PlayerActivityData(activityId, activityData.getRound()));
                //不可领取的设置为领取
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.NOT_CLAIM) {
                    data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                    updateDetailId.add(cfg);
                }
            }
            if (!updateDetailId.isEmpty()) {
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
            }
        } catch (Exception e) {
            log.error("成长基金购买增加进度异常 playerId:{} activityId:{}", playerId, activityId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        if (!updateDetailId.isEmpty()) {
            ResGrowthFundBuyResultInfo info = new ResGrowthFundBuyResultInfo(Code.SUCCESS);
            info.detailInfo = new ArrayList<>(updateDetailId.size());
            for (GrowthFundCfg cfg : updateDetailId) {
                info.detailInfo.add(buildPlayerActivityDetail(player, activityData, cfg, playerActivityData.get(cfg.getId())));
            }
            info.isBuy = true;
            return info;
        }
        return null;
    }

    @Override
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        boolean change = false;
        if (additionalParameters instanceof Integer level) {
            long activityId = activityData.getId();
            //获取配置信息
            Map<Integer, GrowthFundCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
            if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
                return false;
            }
            long playerId = player.getId();
            //加锁前检查是否已经触发完
            Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            if (playerActivityData.size() == baseCfgBeanMap.size()) {
                return false;
            }
            long count = countDao.getCount(String.valueOf(activityData.getId()), String.valueOf(player)).longValue();
            String lockKey = playerActivityDao.getLockKey(playerId, activityId);
            redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
            try {
                playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                for (GrowthFundCfg cfg : baseCfgBeanMap.values()) {
                    //等级不够的跳过
                    if (cfg.getLevel() > level) {
                        continue;
                    }
                    //判断是否能触发
                    if (cfg.getLevel() == ActivityConstant.GrowthFund.FREE || cfg.getType() == ActivityConstant.GrowthFund.Charge && count > 0) {
                        PlayerActivityData data = playerActivityData.computeIfAbsent(cfg.getId(), key -> new PlayerActivityData(activityId, activityData.getRound()));
                        //不可领取的设置为领取
                        if (data.getClaimStatus() == ActivityConstant.ClaimStatus.NOT_CLAIM) {
                            data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                            change = true;
                        }
                    }
                }
                if (change) {
                    playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
                }
            } catch (Exception e) {
                log.error("成长基金增加进度异常 playerId:{} activityId:{}", player, activityId, e);
            } finally {
                redisLock.unlock(lockKey);
            }
        }
        return change;
    }

    /**
     * 玩家每日特权奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @return 返回领取奖励结果
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        long playerId = player.getId();
        long activityId = activityData.getId();
        Map<Integer, GrowthFundCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        GrowthFundCfg cfg = baseCfgBeanMap.get(detailId);
        if (cfg == null || CollectionUtil.isEmpty(cfg.getGetItem())) {
            TipUtils.sendTip(playerId, TipUtils.TipType.TOAST, Code.SAMPLE_ERROR);
            return null;
        }
        ClaimRewardsResult claimRewardsResult = claimActivityRewards(playerId, activityData, detailId, "GrowthFund", cfg.getGetItem());
        // 构建响应数据
        if (claimRewardsResult != null) {
            ResGrowthFundClaimRewards res = new ResGrowthFundClaimRewards(Code.SUCCESS);
            res.activityId = activityId;
            res.detailId = detailId;
            res.infoList = ItemUtils.buildItemInfo(cfg.getGetItem());
            res.detailInfo = buildPlayerActivityDetail(player, activityData, cfg, claimRewardsResult.playerActivityData());
            return res;
        }
        return null;
    }

    /**
     * 构建成长基金活动详情
     *
     * @param player  玩家数据
     * @param activityData 活动ID
     * @param baseCfgBean  活动配置
     * @param data         玩家特权卡数据
     * @return 返回特权卡详情信息
     */
    @Override
    public GrowthFundDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof GrowthFundCfg cfg)) {
            return null;
        }
        GrowthFundDetailInfo info = new GrowthFundDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = baseCfgBean.getId();
        info.type = cfg.getType();
        info.needLevel = cfg.getLevel();
        // 奖励信息
        info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetItem());
        if (data != null) {
            info.claimStatus = data.getClaimStatus();
        }
        return info;
    }

    /**
     * 获取玩家成长基金活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResGrowthFundDetailInfo detailInfo = new ResGrowthFundDetailInfo(Code.SUCCESS);
        //活动数据
        Map<Integer, GrowthFundCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityId);
        detailInfo.detailInfo = new ArrayList<>();
        detailInfo.detailInfo.add(buildPlayerActivityDetail(player, activityData, baseCfgBeanMap.get(detailId), playerActivityData.get(detailId)));
        return detailInfo;
    }

    /**
     * 构建活动类型信息
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResGrowthFundTypeInfo cardTypeInfo = new ResGrowthFundTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            GrowthFundActivityInfo activityInfo = new GrowthFundActivityInfo();
            activityInfo.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(activityInfo);
            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof GrowthFundDetailInfo info) {
                    activityInfo.detailInfos.add(info);
                }
            }
            ActivityData activityData = activityManager.getActivityData().get(entry.getKey());
            if (activityData != null) {
                if (activityData.getBigDecimalParam().size() >= 3) {
                    activityInfo.sellingPrice = activityData.getBigDecimalParam().getFirst().toString();
                    activityInfo.originalPrice = activityData.getBigDecimalParam().get(1).toString();
                    activityInfo.totalGet = activityData.getBigDecimalParam().getFirst().longValue();
                }
                if (CollectionUtil.isNotEmpty(activityData.getChannelCommodity())) {
                    activityInfo.productId = activityData.getChannelCommodity().get(player.getChannel().getValue());
                }
                activityInfo.isBuy = countDao.getCount(String.valueOf(activityData.getId()), String.valueOf(player)).longValue() > 0;
            }
        }
        return cardTypeInfo;
    }

    @Override
    public Map<Integer, GrowthFundCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getGrowthFundCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

    @Override
    public boolean checkPlayerCanJoinActivity(Player player, ActivityData activityData) {
        boolean checked = super.checkPlayerCanJoinActivity(player, activityData);
        if (!checked) {
            return false;
        }
        //已经购买不显示
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return true;
        }
        //全部领取不显示
        Map<Integer, GrowthFundCfg> detailCfgBean = getDetailCfgBean(activityData);
        if (detailCfgBean.size() == playerActivityData.size()) {
            for (PlayerActivityData data : playerActivityData.values()) {
                if (data.getClaimStatus() == ActivityConstant.ActivityStatus.ENDED) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }


    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            Order order = event.getOrder();
            Player player = event.getPlayer();
            if (order.getRechargeType() != RechargeType.GROWTH_FUND) {
                return;
            }
            Map<Long, ActivityData> map = activityManager.getActivityTypeData().get(ActivityType.GROWTH_FUND);
            if (CollectionUtil.isEmpty(map)) {
                log.error("充值事件 没有活动数据 playerId:{} order;{}", player.getId(), JSONObject.toJSONString(order));
                return;
            }
            log.info("充值事件 参加活动 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
            for (ActivityData activityData : map.values()) {
                if (!checkPlayerCanJoinActivity(player, activityData)) {
                    continue;
                }
                if (CollectionUtil.isEmpty(activityData.getChannelCommodity())) {
                    continue;
                }
                String pId = activityData.getChannelCommodity().get(player.getChannel().getValue());
                if (pId.equals(order.getProductId())) {
                    joinActivity(player, activityData, 0, 1);
                    log.info("充值事件 参加活动成功 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
                    break;
                }
            }
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }

}
