package com.jjg.game.activity.common.message.handler;

import com.jjg.game.activity.cashcow.controller.CashCowController;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowFreeRewards;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowRecord;
import com.jjg.game.activity.cashcow.message.req.ReqCashCowTotalPool;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.req.ReqActivityClaimRewards;
import com.jjg.game.activity.common.message.req.ReqActivityDetailInfo;
import com.jjg.game.activity.common.message.req.ReqActivityInfoByType;
import com.jjg.game.activity.common.message.req.ReqActivityPlayerJoin;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/9/3 11:11
 */
@Component
@MessageType(value = MessageConst.MessageTypeDef.ACTIVITY)
public class ActivityMessageHandler {
    private final ActivityManager activityManager;
    private final CashCowController cashCowController;

    public ActivityMessageHandler(ActivityManager activityManager, CashCowController cashCowController) {
        this.activityManager = activityManager;
        this.cashCowController = cashCowController;
    }

    /**
     * 根据活动id,详情id获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_DETAIL_INFO)
    public void reqActivityDetailInfo(PlayerController playerController, ReqActivityDetailInfo req) {
        //查找活动数据
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getValue().contains(req.detailId)) {
            BaseActivityController controller = data.getType().getController();
            if (controller.checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
                AbstractResponse response = controller.getPlayerActivityDetail(playerController.playerId(), data, req.detailId);
                if (response != null) {
                    playerController.send(response);
                }
            }
        }
    }

    /**
     * 根据活动类型获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_INFO_BY_TYPE)
    public void reqActivityInfoByType(PlayerController playerController, ReqActivityInfoByType req) {
        ActivityType activityType = ActivityType.fromType(req.activityType);
        if (activityType == null) {
            return;
        }
        AbstractResponse response = activityType.getController().getPlayerActivityInfoByType(playerController.getPlayer(), activityType);
        if (response != null) {
            playerController.send(response);
        }
    }

    /**
     * 请求领取奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_CLAIM_REWARDS)
    public void reqActivityClaimRewards(PlayerController playerController, ReqActivityClaimRewards req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data == null || !data.getValue().contains(req.detailId)) {
            return;
        }
        BaseActivityController controller = data.getType().getController();
        if (controller.checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
            AbstractResponse response = controller.claimActivityRewards(playerController.playerId(), data, req.detailId);
            if (response != null) {
                playerController.send(response);
            }
        }
    }

    /**
     * 玩家请求参与活动
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_PLAYER_JOIN)
    public void reqActivityPlayerJoin(PlayerController playerController, ReqActivityPlayerJoin req) {
        //查找活动数据
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.getValue().contains(req.detailId) && data.getType().isCanInitiativeJoin()) {
            BaseActivityController controller = data.getType().getController();
            if (controller.checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
                AbstractResponse response = controller.joinActivity(playerController.playerId(), data, req.detailId);
                if (response != null) {
                    playerController.send(response);
                }
            }
        }
    }


    /**
     * 摇钱树请求记录
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_RECORD)
    public void reqCashCowRecord(PlayerController playerController, ReqCashCowRecord req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.canRun() && data.getType() == ActivityType.CASH_COW) {
            if (data.getType().getController().checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
                AbstractResponse res = cashCowController.reqCashCowRecord(playerController, req);
                playerController.send(res);
            }
        }
    }

    /**
     * 摇钱树总奖池
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_TOTAL_POOL)
    public void reqCashCowTotalPool(PlayerController playerController, ReqCashCowTotalPool req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.canRun() && data.getType() == ActivityType.CASH_COW) {
            if (data.getType().getController().checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
                AbstractResponse res = cashCowController.reqCashCowTotalPool(playerController, req);
                playerController.send(res);
            }
        }
    }

    /**
     * 摇钱树请求领取免费奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_CASH_COW_FREE_REWARDS)
    public void reqCashCowFreeRewards(PlayerController playerController, ReqCashCowFreeRewards req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null && data.canRun() && data.getType() == ActivityType.CASH_COW) {
            if (data.getType().getController().checkPlayerCanJoinActivity(playerController.getPlayer(), data)) {
                AbstractResponse res = cashCowController.reqCashCowFreeRewards(playerController, req);
                playerController.send(res);
            }
        }
    }


}
