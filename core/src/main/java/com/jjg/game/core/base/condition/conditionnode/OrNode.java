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

public class OrNode implements ConditionNode {

    private final List<ConditionNode> children;

    public OrNode(List<ConditionNode> children) {
        this.children = children;
    }

    @Override
    public MatchResultData match(ConditionContext ctx) {
        boolean hasUnknown = false;
        MatchResultData result = MatchResultData.notMatch(Code.UNKNOWN_ERROR);
        for (ConditionNode n : children) {
            MatchResultData r = n.match(ctx);
            result = r;
            if (r.result() == MatchResult.MATCH) return MatchResultData.match();
            if (r.result() == MatchResult.UNKNOWN) hasUnknown = true;
        }
        return hasUnknown ? MatchResultData.unknown() : result;
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx) {
        boolean hasUnknown = false;
        int code = 0;
        for (ConditionNode n : children) {
            MatchResultData r = n.addProgress(ctx);
            code = r.errorCode();
            if (r.result() == MatchResult.MATCH) return MatchResultData.match();
            if (r.result() == MatchResult.UNKNOWN) hasUnknown = true;
        }
        return hasUnknown ? MatchResultData.unknown() : MatchResultData.notMatch(code);
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

