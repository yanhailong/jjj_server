package com.jjg.game.activity.common.message.handler;

import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.message.ActivityBuilder;
import com.jjg.game.activity.common.message.req.ReqActivityClaimRewards;
import com.jjg.game.activity.common.message.req.ReqActivityDetailInfo;
import com.jjg.game.activity.common.message.req.ReqActivityInfoByType;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
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

    public ActivityMessageHandler(ActivityManager activityManager) {
        this.activityManager = activityManager;
    }

    /**
     * 根据活动id,详情id获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_DETAIL_INFO)
    public void reqActivityDetailInfo(PlayerController playerController, ReqActivityDetailInfo req) {
        AbstractResponse response = new AbstractResponse(Code.PARAM_ERROR);
        //查找活动数据
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data != null) {
            response = data.getType().getController().getPlayerActivityDetail(playerController.playerId(), data.getId(), req.detailId);
        }
        playerController.send(response);
    }

    /**
     * 根据活动类型获取详细数据
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_INFO_BY_TYPE)
    public void reqActivityInfoByType(PlayerController playerController, ReqActivityInfoByType req) {
        ActivityType activityType = ActivityType.fromType(req.activityType);
        if (activityType == null) {
            AbstractResponse response = new AbstractResponse(Code.PARAM_ERROR);
            playerController.send(response);
            return;
        }
        AbstractResponse response = activityType.getController().getPlayerActivityInfoByType(playerController.playerId(), activityType);
        playerController.send(response);
    }

    /**
     * 请求领取奖励
     */
    @Command(ActivityConstant.MsgBean.REQ_ACTIVITY_CLAIM_REWARDS)
    public void reqActivityClaimRewards(PlayerController playerController, ReqActivityClaimRewards req) {
        ActivityData data = activityManager.getActivityData().get(req.activityId);
        if (data == null || !data.getValue().contains(req.detailId)) {
            playerController.send(ActivityBuilder.getDefaultResponse());
            return;
        }
        AbstractResponse response = data.getType().getController().claimActivityRewards(playerController.playerId(), data, req.detailId);
        playerController.send(response);
    }
}
