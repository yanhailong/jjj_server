package com.jjg.game.activity.bundleGiftPack;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.bundleGiftPack.message.BundleGiftPackDetailInfo;
import com.jjg.game.activity.bundleGiftPack.message.BundleGiftPackInfo;
import com.jjg.game.activity.bundleGiftPack.message.ResBundleGiftPack;
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
import com.jjg.game.sampledata.bean.BundleGiftPackCfg;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 集合礼包
 */
@Component
public class BundleGiftPackController extends BaseActivityController implements OrderGenerate {
    private static final int ALL_GIFT_ID = 0;

    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        return null;
    }

    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    @Override
    public BundleGiftPackDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData,
                                                              BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof BundleGiftPackCfg cfg)) {
            return null;
        }
        Map<Integer, PlayerActivityData> playerData = data == null ? Map.of() : Map.of(cfg.getId(), data);
        return buildActivityDetail(player, activityData, Map.of(cfg.getId(), cfg), playerData);
    }

    @Override
    public List<BaseActivityDetailInfo> getBaseActivityDetailInfos(ActivityData activityData,
                                                                   Map<Integer, ? extends BaseCfgBean> baseCfgBeanMap,
                                                                   Player player,
                                                                   Map<Integer, PlayerActivityData> playerActivityDataMap) {
        BundleGiftPackDetailInfo detailInfo = buildActivityDetail(
                player, activityData, baseCfgBeanMap, playerActivityDataMap);
        return detailInfo == null ? List.of() : List.of(detailInfo);
    }

    private BundleGiftPackDetailInfo buildActivityDetail(Player player, ActivityData activityData,
                                                         Map<Integer, ? extends BaseCfgBean> cfgMap,
                                                         Map<Integer, PlayerActivityData> playerData) {
        if (CollectionUtil.isEmpty(cfgMap)) {
            return null;
        }

        BundleGiftPackDetailInfo detailInfo = new BundleGiftPackDetailInfo();
        detailInfo.activityId = activityData.getId();
        detailInfo.endTime = activityData.getTimeEnd();
        detailInfo.packInfoList = new ArrayList<>(cfgMap.size());

        boolean hasPurchasedGift = CollectionUtil.isNotEmpty(playerData);
        boolean allPurchased = cfgMap.size() == activityData.getValue().size();
        if (allPurchased) {
            for (Integer giftId : activityData.getValue()) {
                if (!isPurchased(playerData.get(giftId))) {
                    allPurchased = false;
                    break;
                }
            }
        }
        for (Integer giftId : activityData.getValue()) {
            if (!(cfgMap.get(giftId) instanceof BundleGiftPackCfg cfg)) {
                continue;
            }
            if (CollectionUtil.isEmpty(cfg.getGetItem())) {
                log.warn("集合礼包奖励配置为空 activityId:{} giftId:{}", activityData.getId(), giftId);
                continue;
            }
            ShopRechargeListCfg shopCfg = GameDataManager.getShopRechargeListCfg(cfg.getChannelCommodity());
            if (!isValidShopCfg(player, shopCfg)) {
                log.warn("集合礼包商品配置错误 activityId:{} giftId:{} shopId:{}",
                        activityData.getId(), giftId, cfg.getChannelCommodity());
                continue;
            }
            BundleGiftPackInfo info = new BundleGiftPackInfo();
            info.id = cfg.getId();
            info.items = ItemUtils.buildItemInfo(cfg.getGetItem());
            info.buyPrice = shopCfg.getPrice().toPlainString();
            info.channelProductId = getChannelProductId(player, shopCfg);
            PlayerActivityData giftData = playerData.get(giftId);
            if (giftData != null) {
                info.claimStatus = giftData.getClaimStatus();
            }
            detailInfo.packInfoList.add(info);
        }

        if (!hasPurchasedGift && cfgMap.size() == activityData.getValue().size()
                && hasConfiguredRewards(cfgMap, cfgMap.keySet())) {
            ShopRechargeListCfg allGiftShopCfg = getAllGiftShopCfg(activityData);
            if (isValidShopCfg(player, allGiftShopCfg)) {
                detailInfo.buyPrice = allGiftShopCfg.getPrice().toPlainString();
                detailInfo.channelProductId = getChannelProductId(player, allGiftShopCfg);
            } else {
                log.warn("集合礼包总购商品配置错误 activityId:{}", activityData.getId());
            }
        }
        if (allPurchased) {
            detailInfo.claimStatus = ActivityConstant.ClaimStatus.CLAIMED;
        }
        return detailInfo;
    }

    @Override
    public ResBundleGiftPack getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        Map<Integer, BundleGiftPackCfg> cfgMap = getDetailCfgBean(activityData);
        Map<Integer, PlayerActivityData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        BundleGiftPackDetailInfo detailInfo = buildActivityDetail(player, activityData, cfgMap, playerData);

        ResBundleGiftPack res = new ResBundleGiftPack(Code.SUCCESS);
        res.activityData = detailInfo == null ? List.of() : List.of(detailInfo);
        return res;
    }

    @Override
    public ResBundleGiftPack getPlayerActivityInfoByTypeRes(Player player,
                                                            Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResBundleGiftPack res = new ResBundleGiftPack(Code.SUCCESS);
        res.activityData = new ArrayList<>(allDetailInfo.size());
        for (List<BaseActivityDetailInfo> detailInfos : allDetailInfo.values()) {
            for (BaseActivityDetailInfo detailInfo : detailInfos) {
                if (detailInfo instanceof BundleGiftPackDetailInfo info) {
                    res.activityData.add(info);
                }
            }
        }
        return res;
    }

    @Override
    public Map<Integer, BundleGiftPackCfg> getDetailCfgBean(ActivityData activityData) {
        if (CollectionUtil.isEmpty(activityData.getValue())) {
            return Map.of();
        }
        Map<Integer, BundleGiftPackCfg> result = new LinkedHashMap<>(activityData.getValue().size());
        Map<Integer, BundleGiftPackCfg> allCfg = GameDataManager.getBundleGiftPackCfgMap();
        for (Integer giftId : activityData.getValue()) {
            BundleGiftPackCfg cfg = allCfg.get(giftId);
            if (cfg != null) {
                result.put(giftId, cfg);
            }
        }
        return result;
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        Integer giftId = parseRequestedGiftId(req.productId);
        if (giftId == null) {
            log.warn("集合礼包下单时获取数据失败 playerId={},productId={}", player.getId(), req.productId);
            return null;
        }
        ActivityData activityData = activityManager.getOpenActivityData(player, ActivityType.BUNDLE_GIFT_PACK);
        if (activityData == null) {
            log.warn("集合礼包下单时获取活动数据失败 playerId={}", player.getId());
            return null;
        }
        if (!isAllGift(giftId) && !activityData.getValue().contains(giftId)) {
            log.warn("集合礼包下单时该礼包id不合法 playerId={},value={},giftId={}", player.getId(), activityData.getValue(), giftId);
            return null;
        }

        PurchaseTarget target = new PurchaseTarget(activityData.getId(), giftId);
        Map<Integer, BundleGiftPackCfg> cfgMap = getDetailCfgBean(activityData);
        ShopRechargeListCfg shopCfg = getPurchaseShopCfg(activityData, cfgMap, target);
        if (!isValidShopCfg(player, shopCfg)) {
            return null;
        }
        Map<Integer, PlayerActivityData> playerData = playerActivityDao.getPlayerActivityData(
                player.getId(), activityData.getType(), activityData.getId());
        if (target.isAllGift()) {
            if (cfgMap.size() != activityData.getValue().size()
                    || !hasConfiguredRewards(cfgMap, cfgMap.keySet()) || CollectionUtil.isNotEmpty(playerData)) {
                return null;
            }
        } else {
            BundleGiftPackCfg cfg = cfgMap.get(target.giftId());
            if (cfg == null || CollectionUtil.isEmpty(cfg.getGetItem())) {
                return null;
            }

            PlayerActivityData playerActivityData = playerData.get(target.giftId());
            if(isPurchased(playerActivityData)){
                log.warn("集合礼包下单时发现该礼包状态错误 playerId={},value={},giftId={},status={}", player.getId(), activityData.getValue(), giftId, playerActivityData == null ? null : playerActivityData.getClaimStatus());
                return null;
            }
        }
        req.productId = target.isAllGift()
                ? String.valueOf(target.activityId()) : target.activityId() + "_" + target.giftId();
        return shopCfg.getPrice();
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.BUNDLE_GIFT_PACK;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        PurchaseTarget target = parseOrderTarget(order.getProductId());
        if (target == null) {
            log.error("集合礼包订单商品ID错误 playerId:{} orderId:{} productId:{}",
                    player.getId(), order.getId(), order.getProductId());
            return false;
        }
        ActivityData activityData = activityManager.getActivityData().get(target.activityId());
        if (!isBundleActivity(player, activityData)) {
            log.error("集合礼包活动不可购买 playerId:{} orderId:{} activityId:{}",
                    player.getId(), order.getId(), target.activityId());
            return false;
        }

        AbstractResponse result = buyActivityGiftForRecharge(player, activityData, target.giftId());
        if (result == null || result.code != Code.SUCCESS) {
            log.error("集合礼包充值处理失败 playerId:{} orderId:{} activityId:{} giftId:{} code:{}",
                    player.getId(), order.getId(), target.activityId(), target.giftId(),
                    result == null ? Code.FAIL : result.code);
            return false;
        }
        try {
            activityManager.sendToPlayer(player.getId(), getPlayerActivityInfoByType(player, ActivityType.BUNDLE_GIFT_PACK));
        } catch (Exception e) {
            log.error("集合礼包购买成功后通知玩家失败 playerId:{} orderId:{} activityId:{}",
                    player.getId(), order.getId(), target.activityId(), e);
        }
        return true;
    }

    @Override
    public AbstractResponse buyActivityGiftForRecharge(Player player, ActivityData activityData, int giftId) {
        ResBundleGiftPack res = new ResBundleGiftPack(Code.SUCCESS);
        Map<Integer, BundleGiftPackCfg> cfgMap = getDetailCfgBean(activityData);
        PurchaseTarget target = new PurchaseTarget(activityData.getId(), giftId);
        ShopRechargeListCfg shopCfg = getPurchaseShopCfg(activityData, cfgMap, target);
        if (cfgMap.isEmpty() || !isValidShopCfg(player, shopCfg)
                || (!target.isAllGift() && !cfgMap.containsKey(giftId))) {
            res.code = Code.PARAM_ERROR;
            return res;
        }

        long playerId = player.getId();
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        boolean locked = false;
        Map<Integer, Long> rewards;
        CommonResult<ItemOperationResult> added;
        try {
            locked = redisLock.tryLockWithDefaultTime(lockKey);
            if (!locked) {
                log.error("集合礼包购买获取锁失败 playerId:{} activityId:{}", playerId, activityData.getId());
                res.code = Code.FAIL;
                return res;
            }

            Map<Integer, PlayerActivityData> playerData = playerActivityDao.getPlayerActivityData(
                    playerId, activityData.getType(), activityData.getId());
            List<Integer> purchasedIds;
            if (target.isAllGift()) {
                if (cfgMap.size() != activityData.getValue().size() || CollectionUtil.isNotEmpty(playerData)) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                purchasedIds = new ArrayList<>(cfgMap.keySet());
            } else {
                if (isPurchased(playerData.get(giftId))) {
                    res.code = Code.REPEAT_OP;
                    return res;
                }
                purchasedIds = List.of(giftId);
            }

            rewards = mergeRewards(cfgMap, purchasedIds);
            if (rewards.isEmpty()) {
                res.code = Code.PARAM_ERROR;
                return res;
            }
            for (Integer purchasedId : purchasedIds) {
                PlayerActivityData data = new PlayerActivityData(activityData.getId(), activityData.getRound());
                data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
                playerData.put(purchasedId, data);
            }
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerData);

            try {
                added = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_BUNDLE_GIFT_PACK);
            } catch (Exception e) {
                rollbackPurchase(playerId, activityData, playerData, purchasedIds);
                throw e;
            }
            if (!added.success()) {
                rollbackPurchase(playerId, activityData, playerData, purchasedIds);
                log.error("集合礼包发放道具失败 playerId:{} activityId:{} giftId:{} code:{}",
                        playerId, activityData.getId(), giftId, added.code);
                res.code = added.code;
                return res;
            }
        } catch (Exception e) {
            log.error("集合礼包购买异常 playerId:{} activityId:{} giftId:{}",
                    playerId, activityData.getId(), giftId, e);
            res.code = Code.FAIL;
            return res;
        } finally {
            if (locked) {
                redisLock.tryUnlock(lockKey);
            }
        }

        activityLogger.sendActivityGift(player, activityData, added.data, rewards, shopCfg.getPrice(), giftId);
        return res;
    }

    private void rollbackPurchase(long playerId, ActivityData activityData,
                                  Map<Integer, PlayerActivityData> playerData, List<Integer> purchasedIds) {
        for (Integer purchasedId : purchasedIds) {
            playerData.remove(purchasedId);
        }
        playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerData);
    }

    private Map<Integer, Long> mergeRewards(Map<Integer, BundleGiftPackCfg> cfgMap, List<Integer> giftIds) {
        Map<Integer, Long> rewards = new HashMap<>();
        for (Integer giftId : giftIds) {
            BundleGiftPackCfg cfg = cfgMap.get(giftId);
            if (cfg == null || CollectionUtil.isEmpty(cfg.getGetItem())) {
                return Map.of();
            }
            cfg.getGetItem().forEach((itemId, count) -> rewards.merge(itemId, count, Long::sum));
        }
        return rewards;
    }

    private boolean hasConfiguredRewards(Map<Integer, ? extends BaseCfgBean> cfgMap, Iterable<Integer> giftIds) {
        for (Integer giftId : giftIds) {
            if (!(cfgMap.get(giftId) instanceof BundleGiftPackCfg cfg)
                    || CollectionUtil.isEmpty(cfg.getGetItem())) {
                return false;
            }
        }
        return true;
    }

    private ShopRechargeListCfg getPurchaseShopCfg(ActivityData activityData,
                                                   Map<Integer, BundleGiftPackCfg> cfgMap,
                                                   PurchaseTarget target) {
        if (target.isAllGift()) {
            return getAllGiftShopCfg(activityData);
        }
        BundleGiftPackCfg cfg = cfgMap.get(target.giftId());
        return cfg == null ? null : GameDataManager.getShopRechargeListCfg(cfg.getChannelCommodity());
    }

    private ShopRechargeListCfg getAllGiftShopCfg(ActivityData activityData) {
        if (CollectionUtil.isEmpty(activityData.getValueParam())) {
            return null;
        }
        long shopId = activityData.getValueParam().getFirst();
        if (shopId < Integer.MIN_VALUE || shopId > Integer.MAX_VALUE) {
            return null;
        }
        return GameDataManager.getShopRechargeListCfg((int) shopId);
    }

    private boolean isBundleActivity(Player player, ActivityData activityData) {
        return activityData != null && activityData.getType() == ActivityType.BUNDLE_GIFT_PACK
                && CollectionUtil.isNotEmpty(activityData.getValue())
                && checkPlayerCanJoinActivity(player, activityData);
    }

    private boolean isValidShopCfg(Player player, ShopRechargeListCfg shopCfg) {
        return shopCfg != null && shopCfg.getPrice() != null
                && StringUtils.isNotBlank(getChannelProductId(player, shopCfg));
    }

    private String getChannelProductId(Player player, ShopRechargeListCfg shopCfg) {
        return player.getChannel() == ChannelType.APPLE ? shopCfg.getIosShopId() : shopCfg.getGoogleShopId();
    }

    private boolean isPurchased(PlayerActivityData data) {
        return data != null && data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED;
    }

    private Integer parseRequestedGiftId(String productId) {
        if (StringUtils.isBlank(productId)) {
            return ALL_GIFT_ID;
        }
        try {
            int giftId = Integer.parseInt(productId.trim());
            return giftId < ALL_GIFT_ID ? null : giftId;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PurchaseTarget parseOrderTarget(String productId) {
        if (StringUtils.isBlank(productId)) {
            return null;
        }
        int separator = productId.indexOf('_');
        try {
            if (separator < 0) {
                return new PurchaseTarget(Long.parseLong(productId), ALL_GIFT_ID);
            }
            if (separator == 0 || separator == productId.length() - 1
                    || productId.indexOf('_', separator + 1) >= 0) {
                return null;
            }
            return new PurchaseTarget(Long.parseLong(productId.substring(0, separator)),
                    Integer.parseInt(productId.substring(separator + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record PurchaseTarget(long activityId, int giftId) {
        private boolean isAllGift() {
            return BundleGiftPackController.isAllGift(giftId);
        }
    }

    private static boolean isAllGift(int giftId) {
        return giftId == ALL_GIFT_ID;
    }
}
