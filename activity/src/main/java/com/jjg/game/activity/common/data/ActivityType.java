package com.jjg.game.activity.common.data;

import com.jjg.game.activity.cashcow.controller.CashCowController;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.dailylogin.controller.DailyLoginController;
import com.jjg.game.activity.dailyrecharge.controller.DailyRechargeController;
import com.jjg.game.activity.firstpayment.controller.FirstPaymentController;
import com.jjg.game.activity.growthfund.controller.GrowthFundController;
import com.jjg.game.activity.officialawards.controller.OfficialAwardsController;
import com.jjg.game.activity.piggybank.controller.PiggyBankController;
import com.jjg.game.activity.privilegecard.controller.PrivilegeCardController;
import com.jjg.game.activity.scratchcards.controller.ScratchCardsController;
import com.jjg.game.activity.sharepromote.controller.SharePromoteController;
import com.jjg.game.common.utils.CommonUtil;

/**
 * @author lm
 * @date 2025/9/3 16:11
 */
public enum ActivityType {
    //每日奖金
    PRIVILEGE_CARD(2, PrivilegeCardController.class, false, false,
            ActivityTargetType.LOGIN.getTargetKey(), false, false, false),
    //摇钱树
    CASH_COW(3, CashCowController.class, true, true,
            ActivityTargetType.EFFECTIVE_BET.getTargetKey(), true, false, true),
    //储钱罐
    PIGGY_BANK(4, PiggyBankController.class, true, false,
            ActivityTargetType.BET.getTargetKey(), false, false, false),
    //刮刮乐
    SCRATCH_CARDS(5, ScratchCardsController.class, false, false,
            ActivityTargetType.NONE.getTargetKey(), true, false, false),
    //推广分享
    SHARE_PROMOTE(6, SharePromoteController.class, true, false,
            ActivityTargetType.getTagetKey(ActivityTargetType.RECHARGE, ActivityTargetType.EFFECTIVE_BET), false, false, false),
    //官方派奖
    OFFICIAL_AWARDS(9, OfficialAwardsController.class, true, false,
            ActivityTargetType.getTagetKey(ActivityTargetType.RECHARGE), true, true, false),
    //每日签到
    DAILY_LOGIN(10, DailyLoginController.class, true, false,
            ActivityTargetType.getTagetKey(ActivityTargetType.LOGIN, ActivityTargetType.RECHARGE, ActivityTargetType.BIND_PHONE), false, false, false),
    //首充
    FIRST_PAYMENT(11, FirstPaymentController.class, false, false,
            ActivityTargetType.RECHARGE.getTargetKey(), false, false, false),
    //每日充值
    DAILY_RECHARGE(12, DailyRechargeController.class, false, false,
            ActivityTargetType.NONE.getTargetKey(), false, false, false),
    //成长基金
    GROWTH_FUND(15, GrowthFundController.class, true, false,
            ActivityTargetType.LEVEL.getTargetKey(), false, false, false);
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
    //活动未开始是否显示
    private final boolean showInNotOpen;
    //活动结束是否清除数据
    private final boolean isClearDataOnEnd;
    ActivityType(int type, Class<? extends BaseActivityController> className, boolean canAddPlayerProgress, boolean canAddActivityProgress,
                 long targetKey, boolean canInitiativeJoin, boolean showInNotOpen, boolean isClearDataOnEnd) {
        this.type = type;
        this.className = className;
        this.canAddPlayerProgress = canAddPlayerProgress;
        this.canAddActivityProgress = canAddActivityProgress;
        this.targetKey = targetKey;
        this.canInitiativeJoin = canInitiativeJoin;
        this.showInNotOpen = showInNotOpen;
        this.isClearDataOnEnd = isClearDataOnEnd;
    }

    public boolean isClearDataOnEnd() {
        return isClearDataOnEnd;
    }

    public boolean isShowInNotOpen() {
        return showInNotOpen;
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

    public static void initialize() {
        for (ActivityType activityType : values()) {
            activityType.controller = CommonUtil.getContext().getBean(activityType.className);
        }
    }
}
