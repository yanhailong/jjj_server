package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 3_大于等于玩家VIP等级
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class PlayerVipLevelCondition implements ConditionHandler<Integer> {

    @Override
    public String type() {
        return "playerVipLevel";
    }

    @Override
    public Integer parse(List<String> args) {
        return Integer.parseInt(args.getFirst());
    }

    @Override
    public MatchResultData match(ConditionContext ctx, Integer level) {
        boolean b = ctx.player().getVipLevel() >= level;
        return b ? MatchResultData.match() : MatchResultData.notMatch(getErrorCode(), level, ctx.player().getVipLevel());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, Integer config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(3);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}

