package com.jjg.game.activity.common.data;

/**
 * 活动触发类型(暂定64种)
 *
 * @author lm
 * @date 2025/9/4 09:51
 */
public enum ActivityTargetType {
    /**
     * 登录
     */
    LOGIN(1, 1),
    /**
     * 等级变化
     */
    LEVEL(2, 1 >> 1),
    /**
     * 重置
     */
    RECHARGE(2, 1 >> 2);
    private final long type;
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
}
