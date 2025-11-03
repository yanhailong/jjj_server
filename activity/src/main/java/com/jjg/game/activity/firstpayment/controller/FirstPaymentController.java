package com.jjg.game.activity.firstpayment.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.firstpayment.message.bean.FirstPaymentActivityInfo;
import com.jjg.game.activity.firstpayment.message.bean.FirstPaymentDetailInfo;
import com.jjg.game.activity.firstpayment.message.res.ResFirstPaymentClaimRewards;
import com.jjg.game.activity.firstpayment.message.res.ResFirstPaymentDetailInfo;
import com.jjg.game.activity.firstpayment.message.res.ResFirstPaymentTypeInfo;
import com.jjg.game.common.pb.AbstractResponse;
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
import com.jjg.game.sampledata.bean.FirstpaymentCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class FirstPaymentController extends BaseActivityController implements GameEventListener, OrderGenerate {

    private final Logger log = LoggerFactory.getLogger(FirstPaymentController.class);


    /**
     * 玩家加入首充活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        加入次数（暂未使用）
     * @return 返回玩家首充活动详情
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        ResFirstPaymentClaimRewards res = new ResFirstPaymentClaimRewards(Code.SUCCESS);
        long playerId = player.getId();
        Map<Integer, FirstpaymentCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        FirstpaymentCfg cfg = baseCfgBeanMap.get(detailId);
        if (cfg == null) {
            log.error("配置错误 未找到首充配置 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.PARAM_ERROR;
            return res;
        }
        PlayerActivityData data;
        CommonResult<ItemOperationResult> addedItems = null;
        Map<Integer, Long> rewards = null;
        String lockKey = playerActivityDao.getLockKey(playerId, activityData.getId());
        // 加锁，防止并发修改
        redisLock.lock(lockKey, ActivityConstant.Common.REDIS_LOCK);
        try {
            Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, activityData.getType(), activityData.getId());
            // 获取玩家首充数据，若不存在则创建
            data = playerActivityData.computeIfAbsent(detailId, key -> new PlayerActivityData(activityData.getId(), activityData.getRound()));

            // 判断玩家是否已购买首充
            if (data.getClaimStatus() != ActivityConstant.ClaimStatus.NOT_CLAIM) {
                log.error("玩家购买过 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                return res;
            }
            // 设置购买时间
            data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
            // 购买奖励发放
            rewards = new HashMap<>(cfg.getGetAvatarFrame());
            rewards.putAll(cfg.getGetitem());
            rewards.putAll(cfg.getGetgold());
            if (CollectionUtil.isNotEmpty(rewards)) {
                //在游戏节点
                addedItems = playerPackService.addItems(playerId, rewards, AddType.ACTIVITY_FIRST_PAYMENT);
                if (!addedItems.success()) {
                    log.error("发放购买奖励失败 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
                }
            }
            // 保存玩家活动数据
            playerActivityDao.savePlayerActivityData(playerId, activityData.getType(), activityData.getId(), playerActivityData);
        } catch (Exception e) {
            log.error("玩家加入首充活动异常 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId, e);
        } finally {
            redisLock.unlock(lockKey);
        }
        if (addedItems != null && addedItems.success()) {
            activityLogger.sendFirstPaymentJoinLog(player, activityData, cfg, addedItems.data, rewards);
        }
        // 发送日志
        if (rewards != null) {
            res.infoList = ItemUtils.buildItemInfo(rewards);
        }
        res.activityId = activityData.getId();
        res.detailId = detailId;
        return res;
    }

    /**
     * 玩家每日领取首充奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @return 返回领取奖励结果
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    /**
     * 构建玩家首充活动详情
     *
     * @param player 玩家数据
     * @param activityData 活动ID
     * @param baseCfgBean  活动配置
     * @param data         玩家首充数据
     * @return 返回首充详情信息
     */
    @Override
    public FirstPaymentDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        if (!(baseCfgBean instanceof FirstpaymentCfg cfg)) {
            return null;
        }
        FirstPaymentDetailInfo info = new FirstPaymentDetailInfo();
        info.activityId = activityData.getId();
        info.detailId = baseCfgBean.getId();
        info.rechargePrice = cfg.getMoney().toPlainString();
        info.wasPrice = cfg.getWasPrice().toPlainString();
        info.bestValue = cfg.getBestValue().toPlainString();
        // 合并总奖励
        Map<Integer, Long> totalGetHashMap = new HashMap<>(cfg.getGetitem());
        totalGetHashMap.putAll(cfg.getGetgold());
        info.rewardItems = ItemUtils.buildItemInfo(totalGetHashMap);
        //购买奖励
        info.bugGet = ItemUtils.buildItemInfo(cfg.getGetAvatarFrame());
        //商品id
        if (CollectionUtil.isNotEmpty(cfg.getChannelCommodity())) {
            info.productId = cfg.getChannelCommodity().get(player.getChannel().getValue());
        }
        if (data != null) {
            info.claimStatus = data.getClaimStatus();
        }

        return info;
    }

    /**
     * 获取玩家首充活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        long activityId = activityData.getId();
        ResFirstPaymentDetailInfo detailInfo = new ResFirstPaymentDetailInfo(Code.SUCCESS);

        Map<Integer, FirstpaymentCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
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
        ResFirstPaymentTypeInfo cardTypeInfo = new ResFirstPaymentTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }

        cardTypeInfo.activityData = new ArrayList<>();
        for (List<BaseActivityDetailInfo> baseActivityDetailInfos : allDetailInfo.values()) {
            FirstPaymentActivityInfo firstPaymentType = new FirstPaymentActivityInfo();
            firstPaymentType.detailInfos = new ArrayList<>();
            cardTypeInfo.activityData.add(firstPaymentType);

            for (BaseActivityDetailInfo baseActivityDetailInfo : baseActivityDetailInfos) {
                if (baseActivityDetailInfo instanceof FirstPaymentDetailInfo info) {
                    firstPaymentType.detailInfos.add(info);
                }
            }
        }

        return cardTypeInfo;
    }


    @Override
    public Map<Integer, FirstpaymentCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getFirstpaymentCfgList()
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
        for (PlayerActivityData data : playerActivityData.values()) {
            if (data.getClaimStatus() == ActivityConstant.ActivityStatus.ENDED) {
                continue;
            }
            return true;
        }
        return false;
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
        if (cfgBean instanceof FirstpaymentCfg cfg) {
            String channelCommodity = cfg.getChannelCommodity().get(player.getChannel().getValue());
            if (channelCommodity == null) {
                return null;
            }
            return cfg.getMoney();
        }
        return null;
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.FIRST_PAYMENT;
    }
}
