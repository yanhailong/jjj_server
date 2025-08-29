package com.jjg.game.core.base.condition;


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

    private boolean numberComparate(long source, long target) {
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
