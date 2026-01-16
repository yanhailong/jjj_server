package com.jjg.game.core.base.condition.conditionnode;

import com.jjg.game.core.base.condition.*;

/**
 * @author lm
 * @date 2026/1/14 10:34
 */
public class AtomicNode<C> implements ConditionNode {

    private final ConditionHandler<C> handler;
    private final C config;

    public AtomicNode(ConditionHandler<C> handler, C config) {
        this.handler = handler;
        this.config = config;
    }

    @Override
    public MatchResultData match(ConditionContext ctx) {
        return handler.match(ctx, config);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx) {
        return handler.addProgress(ctx, config);
    }

    @Override
    public void delete(ConditionContext ctx) {
        handler.delete(ctx, config);
    }

    public ConditionHandler<C> getHandler() {
        return handler;
    }
}

