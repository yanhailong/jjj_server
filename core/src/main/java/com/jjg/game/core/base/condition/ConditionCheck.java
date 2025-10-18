package com.jjg.game.core.base.condition;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/16 17:43
 */
public interface ConditionCheck {
    boolean check(Object param, Object condition);

    /**
     * 添加进度
     *
     * @param param     添加参数
     * @param condition 条件参数
     * @return 添加后的值
     */
    default long addProgress(Object param, Object condition) {
        return 0;
    }

    /**
     * 扣除进度
     *
     * @param param     添加参数
     * @param condition 条件参数
     * @param times     达到池数
     * @return 剩余进度
     */
    default long reduceProgress(Object param, Object condition, long times) {
        return 0;
    }

    default void clearProgress(Object param) {
    }

    long getProgress(Object param);

    Object analysisCondition(List<Integer> condition);
}
