package com.jjg.game.activity.growthfund.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
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
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
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
public class GrowthFundController extends BaseActivityController implements GameEventListener, OrderGenerate {

    private final Logger log = LoggerFactory.getLogger(GrowthFundController.class);

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
        BigDecimal decimal = countDao.incr(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(activityId), String.valueOf(playerId));
        if (decimal.intValue() > 1) {
            log.error("玩家已经购买过成长基金 playerId:{} activityId:{}", playerId, activityId);
            return new ResGrowthFundBuyResultInfo(Code.REPEAT_OP);
        }
        //购买道具奖励
        Map<Integer, Long> rewards = getBuyGetRewards(activityData);
        //获取最新的玩家等级数据
        Player newPlayer = corePlayerService.get(playerId);
        int level = newPlayer.getLevel();
        //获取配置信息
        Map<Integer, GrowthFundCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        //需要更新的详情信息
        Set<GrowthFundCfg> updateDetailId = new HashSet<>();
        Map<Integer, PlayerActivityData> playerActivityData = null;
        CommonResult<ItemOperationResult> addItems = null;
        try {
            playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            //触发需要购买的奖励
            for (GrowthFundCfg cfg : baseCfgBeanMap.values()) {
                //等级不够 和 不需要支付的跳过
                if (cfg.getLevel() > level || cfg.getType() != ActivityConstant.GrowthFund.Charge) {
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
            //道具奖励
            boolean addItemsSuccess = false;
            if (CollectionUtil.isNotEmpty(rewards)) {
                addItems = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_GROWTH_FUND_BUY);
                if (!addItems.success()) {
                    log.error("成长基金购买增加发奖失败 playerId:{} activityId:{}", playerId, activityId);
                } else {
                    addItemsSuccess = true;
                }
            }
            activityLogger.sendGrowthFundBuyLog(player, activityData, activityData.getBigDecimalParam().getLast(),
                    addItemsSuccess ? rewards : null, addItemsSuccess ? addItems.data : null);
        } catch (Exception e) {
            log.error("成长基金购买增加进度异常 playerId:{} activityId:{}", playerId, activityId, e);
        }
        if (!updateDetailId.isEmpty()) {
            ResGrowthFundBuyResultInfo info = new ResGrowthFundBuyResultInfo(Code.SUCCESS);
            info.detailInfo = new ArrayList<>(updateDetailId.size());
            for (GrowthFundCfg cfg : updateDetailId) {
                info.detailInfo.add(buildPlayerActivityDetail(player, activityData, cfg, playerActivityData.get(cfg.getId())));
            }
            if (CollectionUtil.isNotEmpty(rewards)) {
                info.rewards = ItemUtils.buildItemInfo(rewards);
            }
            info.isBuy = true;
            return info;
        }
        return null;
    }

    /**
     *
     * 获取购买能得到的奖励
     *
     * @param activityData 活动数据
     * @return 奖励信息
     */
    private Map<Integer, Long> getBuyGetRewards(ActivityData activityData) {
        Map<Integer, Long> rewards = null;
        try {
            List<Long> valueParam = activityData.getValueParam();
            rewards = new HashMap<>();
            if (CollectionUtil.isNotEmpty(valueParam)) {
                //奖励必须成对
                if (valueParam.size() % 2 == 0) {
                    for (int i = 0; i < valueParam.size(); i++) {
                        rewards.put(valueParam.get(i).intValue(), valueParam.get(++i));
                    }
                }
            }
        } catch (Exception e) {
            log.error("成长基金获取购买获得奖励失败 activityId:{} ", activityData.getId(), e);
        }
        return rewards;
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
            long count = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(activityId), String.valueOf(player.getId())).longValue();
            try {
                playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
                for (GrowthFundCfg cfg : baseCfgBeanMap.values()) {
                    //等级不够的跳过
                    if (cfg.getLevel() > level) {
                        continue;
                    }
                    //判断是否能触发
                    if (cfg.getType() == ActivityConstant.GrowthFund.FREE || cfg.getType() == ActivityConstant.GrowthFund.Charge && count > 0) {
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
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            TipUtils.sendTip(playerId, TipUtils.TipType.TOAST, Code.SAMPLE_ERROR);
            return null;
        }
        ResGrowthFundClaimRewards res = new ResGrowthFundClaimRewards(Code.SUCCESS);
        List<Pair<GrowthFundCfg, PlayerActivityData>> dataPair = new ArrayList<>();
        CommonResult<ItemOperationResult> addedItems = null;
        Map<Integer, Long> rewards = new HashMap<>();
        Map<Integer, PlayerActivityData> dataMap = new HashMap<>();
        //记录日志
        List<Integer> levels = new ArrayList<>();
        try {
            dataMap = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            if (CollectionUtil.isEmpty(dataMap)) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            //全部领取
            for (Map.Entry<Integer, PlayerActivityData> entry : dataMap.entrySet()) {
                PlayerActivityData playerActivityData = entry.getValue();
                if (playerActivityData.getClaimStatus() != ActivityConstant.ClaimStatus.CAN_CLAIM) {
                    continue;
                }
                GrowthFundCfg fundCfg = baseCfgBeanMap.get(entry.getKey());
                if (fundCfg == null || CollectionUtil.isEmpty(fundCfg.getGetItem())) {
                    continue;
                }
                fundCfg.getGetItem().forEach((key, value) -> rewards.merge(key, value, Long::sum));
                playerActivityData.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                dataPair.add(Pair.newPair(fundCfg, playerActivityData));
                levels.add(fundCfg.getLevel());
            }
            if (CollectionUtil.isEmpty(rewards)) {
                res.code = Code.REPEAT_OP;
                return res;
            }
            // 更新状态
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), dataMap);
            // 发放成长基金奖励
            addedItems = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_GROWTH_FUND_CLAIM_REWARDS);
            if (!addedItems.success()) {
                res.code = Code.UNKNOWN_ERROR;
                return res;
            }
        } catch (Exception e) {
            log.error("领取成长基金奖励异常 playerId:{} activityId:{} detailid:{}", playerId, activityData.getId(), detailId, e);
        }
        // 构建响应数据
        res.activityId = activityId;
        res.detailId = detailId;
        if (CollectionUtil.isNotEmpty(rewards)) {
            //发送日志
            if (!levels.isEmpty()) {
                activityLogger.sendGrowthFundReceiveLog(player, activityData, levels, rewards, addedItems);
            }
            res.infoList = ItemUtils.buildItemInfo(rewards);
            res.detailInfo = new ArrayList<>(dataPair.size());
            for (Pair<GrowthFundCfg, PlayerActivityData> pair : dataPair) {
                res.detailInfo.add(buildPlayerActivityDetail(player, activityData, pair.getFirst(), pair.getSecond()));
            }
            res.isAllGet = isAllGet(activityData, dataMap);
        }
        return res;
    }

    /**
     * 构建成长基金活动详情
     *
     * @param player       玩家数据
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
                    activityInfo.sellingPrice = activityData.getBigDecimalParam().getLast().toPlainString();
                    activityInfo.originalPrice = activityData.getBigDecimalParam().get(1).toPlainString();
                    activityInfo.totalGet = activityData.getBigDecimalParam().getFirst().longValue();
                }
                if (CollectionUtil.isNotEmpty(activityData.getChannelCommodity())) {
                    activityInfo.productId = activityData.getChannelCommodity().get(player.getChannel().getValue());
                }
                activityInfo.isBuy = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(activityData.getId()), String.valueOf(player.getId())).longValue() > 0;
                activityInfo.buyGetItems = ItemUtils.buildItemInfo(getBuyGetRewards(activityData));
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
        return !isAllGet(activityData, playerActivityData);
    }

    /**
     * 是否全部领取
     *
     * @param activityData       活动数据
     * @param playerActivityData 玩家获得数据
     * @return true 全部领取 false没全部领取
     */
    private boolean isAllGet(ActivityData activityData, Map<Integer, PlayerActivityData> playerActivityData) {
        //全部领取不显示
        Map<Integer, GrowthFundCfg> detailCfgBean = getDetailCfgBean(activityData);
        if (detailCfgBean.size() == playerActivityData.size()) {
            for (PlayerActivityData data : playerActivityData.values()) {
                if (data.getClaimStatus() == ActivityConstant.ActivityStatus.ENDED) {
                    continue;
                }
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean initProgress(Player player, ActivityData activityData) {
        if (canInitProgress(player.getId(), activityData)) {
            //初始化进度
            addPlayerProgress(player, activityData, player.getLevel(), ActivityTargetType.LEVEL.getTargetKey(), player.getLevel());
            log.info("玩家初始化进度 playerId:{} activityId:{}", player.getId(), activityData.getId());
            return true;
        }
        return false;
    }

    @Override
    public void checkPlayerDataAndResetOnLogin(long playerId, ActivityData activityData) {
        if (activityData.getOpenType() == ActivityConstant.Common.LIMIT_TYPE) {
            return;
        }
        // 获取玩家该活动的历史数据
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerActivityData)) {
            boolean needRest = false;
            for (PlayerActivityData data : playerActivityData.values()) {
                // 如果期数数不一致，则需要重置
                if (data.getRound() != activityData.getRound()) {
                    needRest = true;
                    break;
                }
            }
            if (needRest) {
                playerActivityDao.deletePlayerActivityData(playerId, activityData.getType(), activityData.getId());
                clearInitProgress(playerId, activityData);
            }
        }
    }

    @Override
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(Player player, ActivityData activityData) {
        if (initProgress(player, activityData)) {
            return playerActivityDao.getPlayerActivityData(player.getId(), activityData.getType(), activityData.getId());
        }
        return null;
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
                log.error("充值事件 所有活动数据为空 playerId:{} order;{}", player.getId(), JSONObject.toJSONString(order));
                return;
            }
            ActivityData data = map.get(Long.parseLong(order.getProductId()));
            if (data == null) {
                log.error("充值事件 没有活动数据 playerId:{} order;{}", player.getId(), JSONObject.toJSONString(order));
                return;
            }
            log.info("充值事件 参加活动 playerId:{}  order;{}", player.getId(), JSONObject.toJSONString(order));
            AbstractResponse res = joinActivity(player, data, 1, 1);
            if (res != null) {
                activityManager.sendToPlayer(player.getId(), res);
            }
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        long activityId = Long.parseLong(req.productId);
        ActivityData activityData = activityManager.getActivityData().get(activityId);
        if (activityData == null || !checkPlayerCanJoinActivity(player, activityData)) {
            return null;
        }
        String channelCommodity = activityData.getChannelCommodity().get(player.getChannel().getValue());
        if (channelCommodity == null) {
            return null;
        }
        return activityData.getBigDecimalParam().getLast();
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.GROWTH_FUND;
    }
}
