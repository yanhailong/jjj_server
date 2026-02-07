package com.jjg.game.activity.dailyrecharge.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ClaimRewardsResult;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.dailyrecharge.dao.DailyRechargeDao;
import com.jjg.game.activity.dailyrecharge.message.bean.DailyRechargeActivityInfo;
import com.jjg.game.activity.dailyrecharge.message.bean.DailyRechargeDetailInfo;
import com.jjg.game.activity.dailyrecharge.message.res.ResDailyRechargeClaimRewards;
import com.jjg.game.activity.dailyrecharge.message.res.ResDailyRechargeGiftBuy;
import com.jjg.game.activity.dailyrecharge.message.res.ResDailyRechargeTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
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
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.DailyRechargeCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class DailyRechargeController extends BaseActivityController implements GameEventListener, OrderGenerate {

    public final int MAIL_CFG_ID = 42;
    private final Logger log = LoggerFactory.getLogger(DailyRechargeController.class);
    private final DailyRechargeDao dailyRechargeDao;
    private final String DAILY_RECHARGE = "dailyrecharge";
    private final MailService mailService;

    public DailyRechargeController(DailyRechargeDao dailyRechargeDao, MailService mailService) {
        this.dailyRechargeDao = dailyRechargeDao;
        this.mailService = mailService;
    }

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return null;
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
        ResDailyRechargeClaimRewards res = new ResDailyRechargeClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        Map<Integer, DailyRechargeCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        DailyRechargeCfg cfg = baseCfgBeanMap.get(detailId);
        if (cfg == null || CollectionUtil.isEmpty(cfg.getAwardItem()) || cfg.getType() == ActivityConstant.DailyRecharge.GIFT) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        ClaimRewardsResult claimRewardsResult = claimActivityRewards(playerId, activityData, detailId, AddType.ACTIVITY_DAILY_RECHARGE_PROGRESS, cfg.getAwardItem());
        if (!claimRewardsResult.success()) {
            res.code = claimRewardsResult.code();
            return res;
        }
        // 记录日志并构建返回值
        if (claimRewardsResult.itemOperationResult() != null) {
//                activityLogger.sendCashCowRewards(player, activityData, detailId, claimRewardsResult.itemOperationResult(), activityProgress, cfg.getRewards());
        }
        res.infoList = ItemUtils.buildItemInfo(cfg.getAwardItem());
        res.detailInfo = buildPlayerActivityDetail(player, activityData, cfg, claimRewardsResult.playerActivityData());
        return res;
    }

    /**
     * 构建每日充值活动详情
     *
     * @param player       玩家数据
     * @param activityData 活动ID
     * @param baseCfgBean  活动配置
     * @param data         玩家特权卡数据
     * @return 返回特权卡详情信息
     */
    @Override
    public DailyRechargeDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        return buildPlayerActivityDetail(player, activityData, baseCfgBean, data, null);
    }

    public DailyRechargeDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean,
                                                             PlayerActivityData data, Map<Integer, Integer> buyTimesMap) {
        if (!(baseCfgBean instanceof DailyRechargeCfg cfg)) {
            return null;
        }
        DailyRechargeDetailInfo info = new DailyRechargeDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = baseCfgBean.getId();
        // 奖励信息
        info.rewardItems = ItemUtils.buildItemInfo(cfg.getAwardItem());
        if (data != null) {
            info.claimStatus = data.getClaimStatus();
        }
        info.type = cfg.getType();
        info.extraParameters = cfg.getCost().toPlainString();
        if (cfg.getType() == ActivityConstant.DailyRecharge.GIFT) {
            info.tag = cfg.getLabel1();
            if (buyTimesMap != null) {
                info.currentBuyCount = buyTimesMap.getOrDefault(cfg.getId(), 0);
            } else {
                info.currentBuyCount = dailyRechargeDao.getBuyTimes(player.getId(), activityData.getId(), cfg.getId());
            }
            info.maxBuyCount = cfg.getCount();
        }
        return info;
    }

    /**
     * 获取玩家每日充值活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    /**
     * 构建活动类型信息
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResDailyRechargeTypeInfo cardTypeInfo = new ResDailyRechargeTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        for (Map.Entry<Long, List<BaseActivityDetailInfo>> entry : allDetailInfo.entrySet()) {
            DailyRechargeActivityInfo activityInfo = new DailyRechargeActivityInfo();

            activityInfo.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(activityInfo);

            for (BaseActivityDetailInfo baseActivityDetailInfo : entry.getValue()) {
                if (baseActivityDetailInfo instanceof DailyRechargeDetailInfo info) {
                    activityInfo.detailInfos.add(info);
                }
            }
            BigDecimal progress = countDao.getCount(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(DAILY_RECHARGE),
                    String.valueOf(player.getId()));
            activityInfo.currentRecharge = progress.toPlainString();
            activityInfo.remainTime = TimeHelper.getNextDayRemainTime();
        }
        return cardTypeInfo;
    }

    @Override
    public List<BaseActivityDetailInfo> getBaseActivityDetailInfos(ActivityData activityData, Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap, Player player, Map<Integer, PlayerActivityData> playerActivityDataMap) {
        //获取次数限制
        Map<Integer, Integer> allBuyTimes = dailyRechargeDao.getAllBuyTimes(player.getId(), activityData.getId());
        List<BaseActivityDetailInfo> arrayList = new ArrayList<>();
        for (Integer id : activityData.getValue()) {
            BaseCfgBean baseCfgBean = baseCfgBeanMap.get(id);
            if (baseCfgBean == null) {
                continue;
            }
            BaseActivityDetailInfo detail = buildPlayerActivityDetail(player, activityData, baseCfgBean, playerActivityDataMap.get(id), allBuyTimes);
            if (detail != null) {
                arrayList.add(detail);
            }
        }
        return arrayList;
    }

    @Override
    public void buyActivityGift(Player player, ActivityData activityData, int giftId) {
        //获取配置信息
        long activityId = activityData.getId();
        Map<Integer, DailyRechargeCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            return;
        }
        //发送礼包奖励
        long playerId = player.getId();
        DailyRechargeCfg dailyRechargeCfg = baseCfgBeanMap.get(giftId);
        if (dailyRechargeCfg == null || dailyRechargeCfg.getType() != ActivityConstant.DailyRecharge.GIFT ||
                CollectionUtil.isEmpty(dailyRechargeCfg.getAwardItem())) {
            log.error("每日充值礼包配置错误 playerId:{} activityId:{} giftId:{}", playerId, activityId, giftId);
            return;
        }
        CommonResult<ItemOperationResult> added = playerPackService.addItems(playerId, dailyRechargeCfg.getAwardItem(), AddType.ACTIVITY_DAILY_RECHARGE_GIFT);
        if (!added.success()) {
            log.error("每日充值添加礼包道具失败 playerId:{} activityId:{} giftId:{}", playerId, activityId, giftId);
        }
        ResDailyRechargeGiftBuy res = new ResDailyRechargeGiftBuy(Code.SUCCESS);

        BigDecimal progress = countDao.incrementWithoutExpireRefresh(CountDao.CountType.ACTIVITY_COUNT.getParam().formatted("dailyrecharge"),
                String.valueOf(playerId), dailyRechargeCfg.getCost(), TimeHelper.DAY_SECOND);
        dailyRechargeDao.addBuyTimes(playerId, activityId, giftId);
        res.currentRecharge = progress.toPlainString();
        res.detailInfo = new ArrayList<>();
        res.detailInfo.add(buildPlayerActivityDetail(player, activityData, dailyRechargeCfg, null));
        List<DailyRechargeCfg> list = baseCfgBeanMap.values()
                .stream().filter(cfg ->
                        cfg.getType() == ActivityConstant.DailyRecharge.PROGRESS && cfg.getCost().compareTo(progress) <= 0)
                .toList();
        List<Pair<PlayerActivityData, DailyRechargeCfg>> pairList = getCanTargetDetailInfo(playerId, activityData, list);
        if (!pairList.isEmpty()) {
            for (Pair<PlayerActivityData, DailyRechargeCfg> cfgPair : pairList) {
                res.detailInfo.add(buildPlayerActivityDetail(player, activityData, cfgPair.getSecond(), cfgPair.getFirst()));
            }
        }
        activityManager.sendToPlayer(playerId, res);
    }

    /**
     * 购买礼包后出发进度奖励
     *
     * @param playerId     玩家id
     * @param activityData 活动数据
     * @param canGetCfg    能触发的活动配置
     * @return 触发的详细信息
     */
    private List<Pair<PlayerActivityData, DailyRechargeCfg>> getCanTargetDetailInfo(long playerId, ActivityData activityData, List<DailyRechargeCfg> canGetCfg) {
        //查找能触发的活动详情id
        if (CollectionUtil.isEmpty(canGetCfg)) {
            return List.of();
        }
        long activityId = activityData.getId();
        List<Pair<PlayerActivityData, DailyRechargeCfg>> changeDetail = new ArrayList<>();
        try {
            Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityId);
            for (DailyRechargeCfg rechargeCfg : canGetCfg) {
                PlayerActivityData data = playerActivityData.computeIfAbsent(rechargeCfg.getId(), key -> new PlayerActivityData(activityId, activityData.getRound()));
                if (data.getClaimStatus() == ActivityConstant.ClaimStatus.NOT_CLAIM) {
                    changeDetail.add(Pair.newPair(data, rechargeCfg));
                    data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
                    break;
                }
            }
            if (!changeDetail.isEmpty()) {
                playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityId, playerActivityData);
            }
        } catch (Exception e) {
            log.error("每日充值增加进度异常 playerId:{} activityId:{}", playerId, activityId, e);
        }
        return changeDetail;
    }

    @Override
    public void checkPlayerDataAndResetOnLogin(long playerId, ActivityData activityData) {
        //清除数据
        countDao.reset(playerId, CountDao.CountType.ACTIVITY_COUNT.getParam().formatted(DAILY_RECHARGE), String.valueOf(playerId));
        dailyRechargeDao.delete(playerId, activityData.getId());
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isEmpty(playerActivityData)) {
            return;
        }
        Map<Integer, DailyRechargeCfg> detailCfgBean = getDetailCfgBean(activityData);
        //未领取的发送邮件
        for (Map.Entry<Integer, PlayerActivityData> entry : playerActivityData.entrySet()) {
            try {
                DailyRechargeCfg dailyRechargeCfg = detailCfgBean.get(entry.getKey());
                if (dailyRechargeCfg == null || dailyRechargeCfg.getType() != ActivityConstant.DailyRecharge.GIFT ||
                        CollectionUtil.isEmpty(dailyRechargeCfg.getAwardItem())) {
                    continue;
                }
                mailService.addCfgMail(playerId, MAIL_CFG_ID, ItemUtils.buildItems(dailyRechargeCfg.getAwardItem()));
            } catch (Exception e) {
                log.error("每日充值未领取奖励发送邮件异常 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), entry.getKey(), e);
            }
        }
        playerActivityDao.deletePlayerActivityData(playerId, activityData.getType(), activityData.getId());
    }

    @Override
    public Map<Integer, DailyRechargeCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getDailyRechargeCfgList()
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
            dealActivityRecharge(player, order, 2);
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        BaseCfgBean cfgBean = getOrderGenerateBean(player, req.productId);
        if (cfgBean instanceof DailyRechargeCfg cfg && cfg.getType() == ActivityConstant.DailyRecharge.GIFT) {
            String channelCommodity = cfg.getChannelCommodity().get(player.getChannel().getValue());
            if (channelCommodity == null) {
                return null;
            }
            return cfg.getCost();
        }
        return null;
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.DAILY_RECHARGE;
    }
}
