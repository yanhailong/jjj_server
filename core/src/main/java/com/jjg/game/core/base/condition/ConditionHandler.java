package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:34
 */

import java.util.List;

public interface ConditionHandler<C> {

    String type();

    C parse(List<String> args);

    MatchResultData match(ConditionContext ctx, C config);

    MatchResultData addProgress(ConditionContext ctx, C config);

    int getErrorCode();

    default void delete(ConditionContext ctx, C config) {

    }
}
