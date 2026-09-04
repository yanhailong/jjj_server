package com.jjg.game.activity.buyOneGetSeven;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.activity.buyOneGetSeven.data.BuyOneGetSevenPlayerData;
import com.jjg.game.activity.buyOneGetSeven.message.BuyOneGetSevenDetailInfo;
import com.jjg.game.activity.buyOneGetSeven.message.ResBuyOneGetSeven;
import com.jjg.game.activity.buyOneGetSeven.message.ResBuyOneGetSevenClaimRewards;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.BuyOneGetSevenCfg;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class BuyOneGetSevenController extends BaseActivityController
        implements OrderGenerate, TimerListener<BuyOneGetSevenRedDotTask>, IPlayerLoginSuccess {
    private static final int PURCHASE_DURATION_PARAM_INDEX = 0;
    private static final int UNLOCK_INTERVAL_PARAM_INDEX = 1;
    private static final int UNCLAIMED_REWARD_MAIL_CFG_ID = 49;
    private final TimerCenter timerCenter;
    private final MailService mailService;
    private final Map<BuyOneGetSevenRedDotKey, Long> scheduledRedDotRefresh = new ConcurrentHashMap<>();

    public BuyOneGetSevenController(TimerCenter timerCenter, MailService mailService) {
        this.timerCenter = timerCenter;
        this.mailService = mailService;
    }

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return redisLock.tryLockAndGet(playerActivityDao.getLockKey(player.getId(), activityData.getId()),
                () -> purchaseRewards(player, activityData, detailId), new ResBuyOneGetSevenClaimRewards(Code.FAIL));
    }

    private AbstractResponse purchaseRewards(Player player, ActivityData activityData, int detailId) {
        ResBuyOneGetSevenClaimRewards res = new ResBuyOneGetSevenClaimRewards(Code.FAIL);
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        if (detailId != activityData.getValue().getFirst()) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        long now = System.currentTimeMillis();
        if (!canPurchase(activityData, now)) {
            res.code = Code.ERROR_REQ;
            return res;
        }

        long playerId = player.getId();
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                playerId, activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerData)) {
            res.code = Code.REPEAT_OP;
            return res;
        }

        initializePlayerData(activityData, playerData, now);
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerData);

        BuyOneGetSevenCfg firstRewardCfg = cfgMap.get(detailId);
        CommonResult<ItemOperationResult> added = playerPackService.addItems(
                playerId, firstRewardCfg.getGetItem(), AddType.ACTIVITY_BUY_ONE_GET_SEVEN_BUY);
        if (!added.success()) {
            log.error("买一送七购买奖励发放失败 playerId:{} activityId:{} code:{}",
                    playerId, activityData.getId(), added.code);
            res.code = added.code;
            return res;
        }

        res.code = Code.SUCCESS;
        res.activityId = activityData.getId();
        res.detailId = detailId;
        res.infoList = ItemUtils.buildItemInfo(firstRewardCfg.getGetItem());
        res.activityData = buildDetails(player, activityData, cfgMap, playerData);
        scheduleNextRedDotRefresh(playerId, activityData, playerData, now);
        log.info("买一送七购买成功 playerId:{} activityId:{}", playerId, activityData.getId());
        return res;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return redisLock.tryLockAndGet(playerActivityDao.getLockKey(player.getId(), activityData.getId()),
                () -> claimRewards(player, activityData, detailId), new ResBuyOneGetSevenClaimRewards(Code.FAIL));
    }

    private AbstractResponse claimRewards(Player player, ActivityData activityData, int detailId) {
        ResBuyOneGetSevenClaimRewards res = new ResBuyOneGetSevenClaimRewards(Code.FAIL);
        long now = System.currentTimeMillis();
        if (!activityData.canRun() || now >= activityData.getTimeEnd()) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }

        int rewardIndex = activityData.getValue().indexOf(detailId);
        BuyOneGetSevenCfg cfg = cfgMap.get(detailId);
        if (rewardIndex < 0 || cfg == null) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        long playerId = player.getId();
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                playerId, activityData.getType(), activityData.getId());
        BuyOneGetSevenPlayerData data = playerData.get(detailId);
        if (data == null) {
            res.code = Code.ERROR_REQ;
            return res;
        }

        int claimStatus = claimStatus(data, now);
        if (claimStatus == ActivityConstant.ClaimStatus.CLAIMED) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        if (claimStatus != ActivityConstant.ClaimStatus.CAN_CLAIM
                || !previousRewardClaimed(activityData, playerData, rewardIndex)) {
            res.code = Code.ERROR_REQ;
            return res;
        }

        CommonResult<ItemOperationResult> added = playerPackService.addItems(
                playerId, cfg.getGetItem(), AddType.ACTIVITY_BUY_ONE_GET_SEVEN_CLAIM_REWARD);
        if (!added.success()) {
            res.code = added.code;
            return res;
        }

        data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
        unlockNextReward(activityData, playerData, rewardIndex, now);
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerData);
        scheduleNextRedDotRefresh(playerId, activityData, playerData, now);

        res.code = Code.SUCCESS;
        res.activityId = activityData.getId();
        res.detailId = detailId;
        res.infoList = ItemUtils.buildItemInfo(cfg.getGetItem());
        res.activityData = buildDetails(player, activityData, cfgMap, playerData);
        log.info("买一送七领取成功 playerId:{} activityId:{} detailId:{}",
                playerId, activityData.getId(), detailId);
        return res;
    }

    @Override
    public BuyOneGetSevenDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData,
                                                              BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof BuyOneGetSevenCfg cfg)) {
            return null;
        }
        BuyOneGetSevenDetailInfo info = new BuyOneGetSevenDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = cfg.getId();
        info.rewardItems = ItemUtils.buildItemInfo(cfg.getGetItem());
        if (data instanceof BuyOneGetSevenPlayerData rewardData) {
            info.claimStatus = claimStatus(rewardData, System.currentTimeMillis());
            info.reciveEndTime = rewardData.getUnlockTime();
        }
        return info;
    }

    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        ResBuyOneGetSeven res = buildResponse(player, activityData);
        if (res.code == Code.SUCCESS) {
            res.activityData.removeIf(info -> info.detailId != detailId);
            if (res.activityData.isEmpty()) {
                res.code = Code.PARAM_ERROR;
            }
        }
        return res;
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByType(Player player, ActivityType activityType) {
        ResBuyOneGetSeven res = new ResBuyOneGetSeven(Code.SUCCESS);
        res.activityData = new ArrayList<>();
        if (activityType != ActivityType.BUY_ONE_GET_SEVEN) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        ActivityData activityData = activityManager.getOpenActivityData(player, activityType);
        return activityData == null ? res : buildResponse(player, activityData);
    }

    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player,
                                                           Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        return getPlayerActivityInfoByType(player, ActivityType.BUY_ONE_GET_SEVEN);
    }

    @Override
    public Map<Integer, BuyOneGetSevenCfg> getDetailCfgBean(ActivityData activityData) {
        if (activityData == null || CollectionUtil.isEmpty(activityData.getValue())) {
            return Map.of();
        }
        Map<Integer, BuyOneGetSevenCfg> allCfg = GameDataManager.getBuyOneGetSevenCfgMap();
        Map<Integer, BuyOneGetSevenCfg> result = new LinkedHashMap<>(activityData.getValue().size());
        for (Integer detailId : activityData.getValue()) {
            BuyOneGetSevenCfg cfg = allCfg.get(detailId);
            if (cfg != null) {
                result.put(detailId, cfg);
            }
        }
        return result;
    }

    @Override
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        if (!activityData.canRun()) {
            return false;
        }
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                playerId, activityData.getType(), activityData.getId());
        long now = System.currentTimeMillis();
        return playerData.values().stream()
                .anyMatch(data -> claimStatus(data, now) == ActivityConstant.ClaimStatus.CAN_CLAIM);
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, Account account,
                                     boolean firstLogin) {
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.BUY_ONE_GET_SEVEN);
        if (activityData == null) {
            return;
        }
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        scheduleNextRedDotRefresh(player.getId(), activityData, playerData, System.currentTimeMillis());
    }

    @Override
    public void onTimer(TimerEvent<BuyOneGetSevenRedDotTask> timerEvent) {
        BuyOneGetSevenRedDotTask task = timerEvent.getParameter();
        BuyOneGetSevenRedDotKey key = new BuyOneGetSevenRedDotKey(task.playerId(), task.activityId());
        if (!scheduledRedDotRefresh.remove(key, task.unlockTime())) {
            return;
        }
        ActivityData activityData = activityManager.getActivityData().get(task.activityId());
        if (activityData == null || activityData.getType() != ActivityType.BUY_ONE_GET_SEVEN
                || !activityData.canRun()) {
            return;
        }
        updateRodDot(task.playerId(), activityData, false);
    }

    @Override
    public CommonResult<BigDecimal> generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        CommonResult<BigDecimal> result = new CommonResult<>(Code.FAIL);
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.BUY_ONE_GET_SEVEN);
        if (activityData == null) {
            log.warn("买一送七下单失败，未获取到可参与的活动 playerId={},productId={}",
                    player.getId(), req.productId);
            return result;
        }
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap)) {
            log.warn("买一送七下单失败，活动配置无效 playerId={},productId={},activityId={},value={},valueParam={},cfgIds={}",
                    player.getId(), req.productId, activityData.getId(), activityData.getValue(),
                    activityData.getValueParam(), cfgMap.keySet());
            return result;
        }
        if (!activityManager.playerCanJoinActivity(activityData, player)) {
            log.warn("买一送七下单失败，玩家不满足活动参与条件 playerId={},productId={},activityId={}",
                    player.getId(), req.productId, activityData.getId());
            return result;
        }
        long now = System.currentTimeMillis();
        if (!canPurchase(activityData, now)) {
            log.warn("买一送七下单失败，不在可购买时间内 playerId={},productId={},activityId={},canRun={},now={},timeStart={},timeEnd={},buyEndTime={}",
                    player.getId(), req.productId, activityData.getId(), activityData.canRun(), now,
                    activityData.getTimeStart(), activityData.getTimeEnd(), getBuyEndTime(activityData));
            return result;
        }
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerData)) {
            log.warn("买一送七下单失败，玩家已经购买过该礼包 playerId={},productId={},activityType={},activityId={}",
                    player.getId(), req.productId, activityData.getType(), activityData.getId());
            return result;
        }
        ShopRechargeListCfg shopCfg = getShopCfg(activityData);
        if (shopCfg == null) {
            log.warn("买一送七下单失败，未找到充值商品配置 playerId={},productId={},activityId={},channelCommodity={}",
                    player.getId(), req.productId, activityData.getId(), activityData.getChannelCommodity());
            return result;
        }
        if (shopCfg.getPrice() == null) {
            log.warn("买一送七下单失败，充值商品价格为空 playerId={},productId={},activityId={},channelCommodity={}",
                    player.getId(), req.productId, activityData.getId(), activityData.getChannelCommodity());
            return result;
        }
        if (StringUtils.isBlank(getChannelProductId(player, shopCfg))) {
            log.warn("买一送七下单失败，渠道商品ID为空 playerId={},productId={},activityId={},channel={},channelCommodity={}",
                    player.getId(), req.productId, activityData.getId(), player.getChannel(),
                    activityData.getChannelCommodity());
            return result;
        }
        result.code = Code.SUCCESS;
        result.data = shopCfg.getPrice();
        return result;
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.BUY_ONE_GET_SEVEN;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.BUY_ONE_GET_SEVEN);
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap) || !activityManager.playerCanJoinActivity(activityData, player)) {
            log.error("买一送七充值回调活动无效 playerId:{} order:{}", player.getId(), JSONObject.toJSONString(order));
            return false;
        }
        AbstractResponse res = joinActivity(player, activityData, activityData.getValue().getFirst(), 1);
        if (res == null || res.code != Code.SUCCESS) {
            log.error("买一送七充值处理失败 playerId:{} order:{} code:{}",
                    player.getId(), JSONObject.toJSONString(order), res == null ? Code.FAIL : res.code);
            return false;
        }
        activityManager.sendToPlayer(player.getId(), res);
        updateRodDot(player.getId(), activityData, false);
        return true;
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        super.onActivityStart(activityData);
        playerActivityDao.clearActivityData(ActivityType.BUY_ONE_GET_SEVEN, activityData.getId());
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        super.onActivityEnd(activityData);
        scheduledRedDotRefresh.keySet().removeIf(key -> key.activityId() == activityData.getId());
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap)) {
            log.error("买一送七结束补发配置无效 activityId:{}", activityData.getId());
            return;
        }
        long endTime = activityData.getTimeEnd();
        long round = activityData.getRound();
        playerActivityDao.<BuyOneGetSevenPlayerData>clearActivityData(
                ActivityType.BUY_ONE_GET_SEVEN, activityData.getId(), (playerId, playerData) -> {
                    Map<Integer, Long> rewards = new HashMap<>();
                    for (Map.Entry<Integer, BuyOneGetSevenPlayerData> entry : playerData.entrySet()) {
                        BuyOneGetSevenPlayerData data = entry.getValue();
                        if (data.getRound() != round) {
                            return false;
                        }
                        // 只按本期结束时已有的解锁时间补发，不推进后续奖励的解锁。
                        if (claimStatus(data, endTime) == ActivityConstant.ClaimStatus.CAN_CLAIM) {
                            ItemUtils.mergeItems(rewards, cfgMap.get(entry.getKey()).getGetItem());
                        }
                    }
                    if (!rewards.isEmpty()) {
                        List<Item> items = ItemUtils.buildItems(rewards);
                        mailService.addCfgMail(playerId, UNCLAIMED_REWARD_MAIL_CFG_ID, items, AddType.ACTIVITY_BUY_ONE_GET_SEVEN_CLAIM_REWARD);
                    }
                    return true;
                });
    }

    private void initializePlayerData(ActivityData activityData,
                                      Map<Integer, BuyOneGetSevenPlayerData> playerData, long now) {
        for (int index = 0; index < activityData.getValue().size(); index++) {
            int rewardId = activityData.getValue().get(index);
            BuyOneGetSevenPlayerData data = new BuyOneGetSevenPlayerData(
                    activityData.getId(), activityData.getRound());
            if (index == 0) {
                data.setUnlockTime(now);
                data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
            } else if (index == 1) {
                data.setUnlockTime(now + getUnlockIntervalMillis(activityData));
            }
            playerData.put(rewardId, data);
        }
    }

    private ResBuyOneGetSeven buildResponse(Player player, ActivityData activityData) {
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        return buildResponse(player, activityData, cfgMap, playerData);
    }

    private ResBuyOneGetSeven buildResponse(Player player, ActivityData activityData,
                                            Map<Integer, BuyOneGetSevenCfg> cfgMap,
                                            Map<Integer, BuyOneGetSevenPlayerData> playerData) {
        ResBuyOneGetSeven res = new ResBuyOneGetSeven(Code.SUCCESS);
        res.activityData = new ArrayList<>();
        if (!validConfig(activityData, cfgMap)) {
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        res.activityId = activityData.getId();
        res.activityEndTime = activityData.getTimeEnd();
        res.buyEndTime = getBuyEndTime(activityData);
        res.isBuy = CollectionUtil.isNotEmpty(playerData);
        res.activityData = buildDetails(player, activityData, cfgMap, playerData);

        ShopRechargeListCfg shopCfg = getShopCfg(activityData);
        if (shopCfg != null && shopCfg.getPrice() != null) {
            res.sellingPrice = shopCfg.getPrice().toPlainString();
            res.productId = getChannelProductId(player, shopCfg);
        }
        return res;
    }

    private List<BuyOneGetSevenDetailInfo> buildDetails(Player player, ActivityData activityData,
                                                        Map<Integer, BuyOneGetSevenCfg> cfgMap,
                                                        Map<Integer, BuyOneGetSevenPlayerData> playerData) {
        List<BuyOneGetSevenDetailInfo> details = new ArrayList<>(activityData.getValue().size());
        for (Integer detailId : activityData.getValue()) {
            BuyOneGetSevenDetailInfo info = buildPlayerActivityDetail(
                    player, activityData, cfgMap.get(detailId), playerData.get(detailId));
            if (info != null) {
                details.add(info);
            }
        }
        return details;
    }

    private void unlockNextReward(ActivityData activityData, Map<Integer, BuyOneGetSevenPlayerData> playerData,
                                  int rewardIndex, long now) {
        int nextIndex = rewardIndex + 1;
        if (nextIndex >= activityData.getValue().size()) {
            return;
        }
        BuyOneGetSevenPlayerData nextData = playerData.get(activityData.getValue().get(nextIndex));
        if (nextData == null || nextData.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
            return;
        }
        long unlockTime = now + getUnlockIntervalMillis(activityData);
        nextData.setUnlockTime(unlockTime);
        nextData.setClaimStatus(unlockTime <= now
                ? ActivityConstant.ClaimStatus.CAN_CLAIM
                : ActivityConstant.ClaimStatus.NOT_CLAIM);
    }

    /**
     * 为下一份尚未解锁的免费奖励预约红点刷新。
     */
    private void scheduleNextRedDotRefresh(long playerId, ActivityData activityData,
                                           Map<Integer, BuyOneGetSevenPlayerData> playerData, long now) {
        long nextUnlockTime = playerData.values().stream()
                .filter(data -> data.getClaimStatus() != ActivityConstant.ClaimStatus.CLAIMED)
                .mapToLong(BuyOneGetSevenPlayerData::getUnlockTime)
                .filter(unlockTime -> unlockTime > now && unlockTime <= activityData.getTimeEnd())
                .min()
                .orElse(0L);
        if (nextUnlockTime <= 0) {
            return;
        }

        BuyOneGetSevenRedDotKey key = new BuyOneGetSevenRedDotKey(playerId, activityData.getId());
        Long scheduledTime = scheduledRedDotRefresh.get(key);
        if (scheduledTime != null && scheduledTime > now && scheduledTime <= nextUnlockTime) {
            return;
        }
        scheduledRedDotRefresh.put(key, nextUnlockTime);
        timerCenter.add(new TimerEvent<>(this, nextUnlockTime,
                new BuyOneGetSevenRedDotTask(playerId, activityData.getId(), nextUnlockTime)));
    }

    private boolean previousRewardClaimed(ActivityData activityData,
                                          Map<Integer, BuyOneGetSevenPlayerData> playerData, int rewardIndex) {
        if (rewardIndex == 0) {
            return true;
        }
        BuyOneGetSevenPlayerData previous = playerData.get(activityData.getValue().get(rewardIndex - 1));
        return previous != null && previous.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED;
    }

    private int claimStatus(BuyOneGetSevenPlayerData data, long now) {
        if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
            return ActivityConstant.ClaimStatus.CLAIMED;
        }
        if (data.getUnlockTime() > 0 && now >= data.getUnlockTime()) {
            return ActivityConstant.ClaimStatus.CAN_CLAIM;
        }
        return ActivityConstant.ClaimStatus.NOT_CLAIM;
    }

    private boolean validConfig(ActivityData activityData, Map<Integer, BuyOneGetSevenCfg> cfgMap) {
        if (activityData == null || CollectionUtil.isEmpty(activityData.getValue())
                || activityData.getValueParam() == null
                || activityData.getValueParam().size() <= UNLOCK_INTERVAL_PARAM_INDEX
                || activityData.getValueParam().get(PURCHASE_DURATION_PARAM_INDEX) < 0
                || activityData.getValueParam().get(UNLOCK_INTERVAL_PARAM_INDEX) < 0
                || cfgMap.size() != activityData.getValue().size()) {
            return false;
        }
        return cfgMap.values().stream().allMatch(cfg -> CollectionUtil.isNotEmpty(cfg.getGetItem()));
    }

    private boolean canPurchase(ActivityData activityData, long now) {
        return activityData.canRun() && now >= activityData.getTimeStart()
                && now < activityData.getTimeEnd() && now <= getBuyEndTime(activityData);
    }

    private long getBuyEndTime(ActivityData activityData) {
        long durationMillis = TimeUnit.SECONDS.toMillis(
                activityData.getValueParam().get(PURCHASE_DURATION_PARAM_INDEX));
        return Math.min(activityData.getTimeEnd(), activityData.getTimeStart() + durationMillis);
    }

    private long getUnlockIntervalMillis(ActivityData activityData) {
        return TimeUnit.SECONDS.toMillis(activityData.getValueParam().get(UNLOCK_INTERVAL_PARAM_INDEX));
    }

    private ShopRechargeListCfg getShopCfg(ActivityData activityData) {
        return GameDataManager.getShopRechargeListCfg(activityData.getChannelCommodity());
    }

    private String getChannelProductId(Player player, ShopRechargeListCfg shopCfg) {
        return player.getChannel() == ChannelType.APPLE ? shopCfg.getIosShopId() : shopCfg.getGoogleShopId();
    }
}

record BuyOneGetSevenRedDotKey(long playerId, long activityId) {
}

record BuyOneGetSevenRedDotTask(long playerId, long activityId, long unlockTime) {
}
