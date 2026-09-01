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
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.ChannelType;
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
import com.jjg.game.sampledata.bean.BuyOneGetSevenCfg;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class BuyOneGetSevenController extends BaseActivityController implements OrderGenerate {
    private static final int PURCHASE_DURATION_PARAM_INDEX = 0;
    private static final int UNLOCK_INTERVAL_PARAM_INDEX = 1;

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
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
        log.info("买一送七购买成功 playerId:{} activityId:{}", playerId, activityData.getId());
        return res;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        ResBuyOneGetSevenClaimRewards res = new ResBuyOneGetSevenClaimRewards(Code.FAIL);
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

        long now = System.currentTimeMillis();
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
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.BUY_ONE_GET_SEVEN);
        Map<Integer, BuyOneGetSevenCfg> cfgMap = getDetailCfgBean(activityData);
        if (!validConfig(activityData, cfgMap) || !activityManager.playerCanJoinActivity(activityData, player)
                || !canPurchase(activityData, System.currentTimeMillis())) {
            return null;
        }
        Map<Integer, BuyOneGetSevenPlayerData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        if (CollectionUtil.isNotEmpty(playerData)) {
            return null;
        }
        ShopRechargeListCfg shopCfg = getShopCfg(activityData);
        if (shopCfg == null || shopCfg.getPrice() == null
                || StringUtils.isBlank(getChannelProductId(player, shopCfg))) {
            return null;
        }
        return shopCfg.getPrice();
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
        playerActivityDao.clearActivityData(ActivityType.BUY_ONE_GET_SEVEN, activityData.getId());
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        playerActivityDao.clearActivityData(ActivityType.BUY_ONE_GET_SEVEN, activityData.getId());
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
        return activityData.canRun() && now >= activityData.getTimeStart() && now <= getBuyEndTime(activityData);
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
