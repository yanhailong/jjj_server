package com.jjg.game.activity.common.data;

import com.jjg.game.activity.cashcow.controller.CashCowController;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.privilegecard.controller.PrivilegeCardController;
import com.jjg.game.common.utils.CommonUtil;

/**
 * @author lm
 * @date 2025/9/3 16:11
 */
public enum ActivityType {
    //每日奖金
    PRIVILEGE_CARD(2, PrivilegeCardController.class, false, false,
            ActivityTargetType.LOGIN.getTargetKey(), false),
    //摇钱树
    CASH_COW(3, CashCowController.class, true, true,
            ActivityTargetType.EFFECTIVE_BET.getTargetKey(), true);
    //活动类型
    private final int type;
    //活动控制器的class
    private final Class<? extends BaseActivityController> className;
    //是否能增加玩家几点
    private final boolean canAddPlayerProgress;
    //是否能增加活动进度
    private final boolean canAddActivityProgress;
    //活动对应控制器
    private BaseActivityController controller;
    //触发的key
    private final long targetKey;
    //是否能主动参加活动
    private final boolean canInitiativeJoin;
    ActivityType(int type, Class<? extends BaseActivityController> className, boolean canAddPlayerProgress, boolean canAddActivityProgress, long targetKey, boolean canInitiativeJoin) {
        this.type = type;
        this.className = className;
        this.canAddPlayerProgress = canAddPlayerProgress;
        this.canAddActivityProgress = canAddActivityProgress;
        this.targetKey = targetKey;
        this.canInitiativeJoin = canInitiativeJoin;
    }

    public Class<? extends BaseActivityController> getClassName() {
        return className;
    }

    public boolean isCanAddActivityProgress() {
        return canAddActivityProgress;
    }

    public boolean isCanAddPlayerProgress() {
        return canAddPlayerProgress;
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

    public boolean isCanInitiativeJoin() {
        return canInitiativeJoin;
    }

    public static void intialize() {
        for (ActivityType activityType : values()) {
            activityType.controller = CommonUtil.getContext().getBean(activityType.className);
        }
    }
}
