package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
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
public class PlayerVipLevelCondition implements ConditionHandler<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    public PlayerVipLevelCondition(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "playerVipLevel";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.PLAYER_VIP_LEVEL;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(3, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        int level = ctx.player().getVipLevel();
        ConditionUpdate update = config.evaluate(
                new StateConditionEvent(StateConditionEvent.Type.VIP_LEVEL, 0, level, 0));
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), level);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
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

