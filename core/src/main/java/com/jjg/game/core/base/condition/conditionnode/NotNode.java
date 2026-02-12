package com.jjg.game.core.base.condition.conditionnode;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionNode;
import com.jjg.game.core.base.condition.MatchResultData;

/**
 * @author lm
 * @date 2026/1/14 10:33
 */
public class NotNode implements ConditionNode {

    private final ConditionNode child;

    public NotNode(ConditionNode child) {
        this.child = child;
    }

    @Override
    public MatchResultData match(ConditionContext ctx) {
        MatchResultData r = child.match(ctx);
        return switch (r.result()) {
            case MATCH -> MatchResultData.notMatch(r.errorCode());
            case NOT_MATCH -> MatchResultData.match();
            case UNKNOWN -> MatchResultData.unknown();
        };
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx) {
        MatchResultData r = child.addProgress(ctx);
        return switch (r.result()) {
            case MATCH -> MatchResultData.notMatch(r.errorCode());
            case NOT_MATCH -> MatchResultData.match();
            case UNKNOWN -> MatchResultData.unknown();
        };
    }

    @Override
    public void delete(ConditionContext ctx) {
        child.delete(ctx);
    }

    public ConditionNode getChild() {
        return child;
    }
}

