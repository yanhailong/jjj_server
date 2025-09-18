package com.jjg.game.activity.common.message;

import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.message.bean.ActivityInfo;

/**
 * @author lm
 * @date 2025/9/5 10:13
 */
public class ActivityBuilder {


    public static ActivityInfo buildActivityInfo(ActivityData data, int claimStatus) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.activityId = data.getId();
        activityInfo.activityType = data.getType().getType();
        activityInfo.status = data.getStatus();
        return activityInfo;
    }
}
