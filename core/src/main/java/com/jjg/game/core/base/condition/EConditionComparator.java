package com.jjg.game.core.base.condition;


import java.util.Collection;

/**
 * 条件比较枚举
 *
 * @author 2CL
 */
public enum EConditionComparator {
    // 大于
    GT,
    // 小于
    LT,
    // 等于
    EQ,
    // 大于等于
    GTE,
    // 小于等于
    LTE,
    // 在范围内
    IN,
    // 不在范围中
    NOT_IN,
    ;

    public boolean intComparate(int source, int target) {
        return numberComparate(source, target);
    }

    public boolean longComparate(long source, long target) {
        return numberComparate(source, target);
    }

    public boolean byteComparate(byte source, byte target) {
        return numberComparate(source, target);
    }

    public boolean shortComparate(short source, short target) {
        return numberComparate(source, target);
    }

    public boolean floatComparate(float source, float target) {
        return numberComparate(source, target);
    }

    public boolean doubleComparate(double source, double target) {
        return numberComparate(source, target);
    }

    public <T> boolean inRange(Collection<T> source, T target) {
        return switch (this) {
            case IN -> source.contains(target);
            default -> false;
        };
    }

    public <T> boolean notinRange(Collection<T> source, T target) {
        return switch (this) {
            case NOT_IN -> !source.contains(target);
            default -> false;
        };
    }

    private boolean numberComparate(long source, long target) {
        return switch (this) {
            case GT -> source < target;
            case LT -> source > target;
            case EQ -> source == target;
            case GTE -> source >= target;
            case LTE -> source <= target;
            default -> false;
        };
    }

    private boolean numberComparate(double source, double target) {
        return switch (this) {
            case GT -> source < target;
            case LT -> source > target;
            case EQ -> source == target;
            case GTE -> source <= target;
            case LTE -> source >= target;
            default -> false;
        };
    }
}
