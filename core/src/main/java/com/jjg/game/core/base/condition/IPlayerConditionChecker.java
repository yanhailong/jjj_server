package com.jjg.game.core.base.condition;

import com.jjg.game.core.data.Player;

import java.util.Map;

/**
 * 玩家条件检查
 *
 * @author 2CL
 */
public interface IPlayerConditionChecker extends IConditionChecker {

    /**
     * 条件检查
     *
     * @param player              player
     * @param comparatorTaget     被比较目标
     * @param conditionComparator 条件比较枚举
     * @return 检查结果
     */
    boolean check(Player player, Map<String, Object> comparatorTaget, EConditionComparator conditionComparator);

    /**
     * 条件检查, 默认比较参数全等
     *
     * @param player          player
     * @param comparatorTaget 被比较目标
     * @return 检查结果
     */
    boolean check(Player player, Map<String, Object> comparatorTaget);
}
