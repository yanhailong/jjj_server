package com.jjg.game.core.base.condition;

import java.util.Map;

/**
 * 游戏条件
 *
 * @author 2CL
 */
public interface IGameConditionChecker extends IConditionChecker {

    /**
     * 条件检查
     *
     * @param comparatorSource 比较源
     * @param comparatorTaget  被比较目标
     * @param comparator       条件比较枚举
     * @return 检查结果
     */
    boolean check(Object comparatorSource, Map<String, Object> comparatorTaget, EConditionComparator comparator);


    /**
     * 条件检查, 默认比较参数全等
     *
     * @param comparatorSource 比较源
     * @param comparatorTaget  被比较目标
     * @return 检查结果
     */
    boolean check(Object comparatorSource, Map<String, Object> comparatorTaget);
}
