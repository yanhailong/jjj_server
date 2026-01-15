package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:32
 */
public interface ConditionNode {

    MatchResultData match(ConditionContext ctx);

    MatchResultData addProgress(ConditionContext ctx);

    void delete(ConditionContext conditionContext);
}
