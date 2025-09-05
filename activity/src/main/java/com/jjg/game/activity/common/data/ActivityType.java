package com.jjg.game.activity.common.data;

import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.privilegecard.controller.PrivilegeCardController;
import com.jjg.game.common.utils.CommonUtil;

/**
 * @author lm
 * @date 2025/9/3 16:11
 */
public enum ActivityType {
    PRIVILEGE_CARD(2, PrivilegeCardController.class, ActivityTargetType.LOGIN.getTargetKey());
    private final int type;
    private final Class<? extends BaseActivityController> className;
    private BaseActivityController controller;
    private final long targetKey;
    ActivityType(int type, Class<? extends BaseActivityController> className, long targetKey) {
        this.type = type;
        this.className = className;
        this.targetKey = targetKey;
    }

    public int getType() {
        return type;
    }

    public BaseActivityController getController() {
        return controller;
    }

    public long getTargetKey() {
        return targetKey;
    }

    public static ActivityType fromType(int type) {
        for (ActivityType at : values()) {
            if (at.getType() == type) {
                return at;
            }
        }
        return null;
    }

    public static void intialize() {
        for (ActivityType activityType : values()) {
            activityType.controller = CommonUtil.getContext().getBean(activityType.className);
        }
    }
}
