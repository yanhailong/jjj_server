package com.jjg.game.activity.common.message;

import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.core.constant.Code;

/**
 * @author lm
 * @date 2025/9/5 10:13
 */
public class ActivityBuilder {

    private final static AbstractResponse defaultResponse = new AbstractResponse(Code.ERROR_REQ);

    /**
     * 获取活动默认响应
     *
     * @return
     */
    public static AbstractResponse getDefaultResponse() {
        return defaultResponse;
    }

    public static ActivityInfo buildActivityInfo(ActivityData data, int claimStatus) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.activityId = data.getId();
        activityInfo.activityType = data.getType().getType();
        activityInfo.status = data.getStatus();
        activityInfo.claimStatus = claimStatus;
        return activityInfo;
    }
}
