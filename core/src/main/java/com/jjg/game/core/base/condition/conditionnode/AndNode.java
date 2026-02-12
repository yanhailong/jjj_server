package com.jjg.game.core.base.condition.conditionnode;

/**
 * @author lm
 * @date 2026/1/14 10:33
 */

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionNode;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.constant.Code;

import java.util.List;

public class AndNode implements ConditionNode {

    private final List<ConditionNode> children;

    public AndNode(List<ConditionNode> children) {
        this.children = children;
    }

    @Override
    public MatchResultData match(ConditionContext ctx) {
        boolean hasUnknown = false;
        for (ConditionNode n : children) {
            MatchResultData r = n.match(ctx);
            if (r.result() == MatchResult.NOT_MATCH) return r;
            if (r.result() == MatchResult.UNKNOWN) hasUnknown = true;
        }
        return hasUnknown ? MatchResultData.unknown() : MatchResultData.match();
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx) {
        boolean notMatch = false;
        boolean hasUnknown = false;
        for (ConditionNode n : children) {
            MatchResultData r = n.addProgress(ctx);
            if (r.result() == MatchResult.NOT_MATCH) notMatch = true;
            if (r.result() == MatchResult.UNKNOWN) hasUnknown = true;
        }
        if (notMatch) {
            return MatchResultData.notMatch(Code.FAIL);
        }
        return hasUnknown ? MatchResultData.unknown() : MatchResultData.match();
    }

    @Override
    public void delete(ConditionContext ctx) {
        for (ConditionNode n : children) {
            n.delete(ctx);
        }
    }

    public List<ConditionNode> getChildren() {
        return children;
    }
}
