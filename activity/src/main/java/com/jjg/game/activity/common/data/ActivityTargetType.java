package com.jjg.game.activity.common.data;

/**
 * 活动触发类型(暂定64种)
 *
 * @author lm
 * @date 2025/9/4 09:51
 */
public enum ActivityTargetType {
    /**
     * 无
     */
    NONE(0, 0),
    /**
     * 登录
     */
    LOGIN(1, 1),
    /**
     * 等级变化
     */
    LEVEL(2, 1 << 1),
    /**
     * 充值
     */
    RECHARGE(3, 1 << 2),
    /**
     * 有效押注金币
     */
    EFFECTIVE_BET(4, 1 << 3),
    /**
     * 押注金币
     */
    BET(5, 1 << 4),
    /**
     * 绑定手机
     */
    BIND_PHONE(6, 1 << 5);
    //触发类型
    private final long type;
    //触发key
    private final long targetKey;

    ActivityTargetType(long type, long targetKey) {
        this.type = type;
        this.targetKey = targetKey;
    }

    public long getType() {
        return type;
    }

    public long getTargetKey() {
        return targetKey;
    }


    public static long getTagetKey(ActivityTargetType... targetType) {
        long tagetKey = 0;
        for (ActivityTargetType type : targetType) {
            tagetKey = type.getTargetKey() | tagetKey;
        }
        return tagetKey;
    }


}
