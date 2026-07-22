package com.jjg.game.core.base.condition.numeric;

/**
 * 一次条件求值结果。未匹配的事件不会改变进度；匹配后由业务模块决定何时、向何处持久化。
 */
public record ConditionUpdate(boolean matched, ProgressMode mode, long value, long target) {

    public static ConditionUpdate ignored(long target) {
        return new ConditionUpdate(false, ProgressMode.ADD, 0, target);
    }

    public static ConditionUpdate matched(ProgressMode mode, long value, long target) {
        return new ConditionUpdate(true, mode, value, target);
    }

    /**
     * 在内存中应用本次更新。ADD 使用饱和加法，防止长期累计条件发生 long 溢出。
     */
    public long apply(long current) {
        if (!matched) {
            return current;
        }
        return switch (mode) {
            case ADD -> addSaturated(current, Math.max(0, value));
            case MAX -> Math.max(current, value);
            case SET -> value;
        };
    }

    public boolean completed(long current) {
        return current >= target;
    }

    private static long addSaturated(long left, long right) {
        if (right <= 0) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
