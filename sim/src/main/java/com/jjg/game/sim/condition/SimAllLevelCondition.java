package com.jjg.game.sim.condition;

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
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模拟经营所有场景等级之和大于等于目标等级。
 */
@Component
public class SimAllLevelCondition implements ConditionHandler<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;
    private final SimPlayerContextRegistry contextRegistry;

    public SimAllLevelCondition(ConditionRuleRegistry conditionRules, SimPlayerContextRegistry contextRegistry) {
        this.conditionRules = conditionRules;
        this.contextRegistry = contextRegistry;
    }

    @Override
    public String type() {
        return "simAllLevel";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.SIM_ALL_LEVEL;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(1, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        SimPlayerContext simContext = contextRegistry.getContext(ctx.player().getId());
        int allLevel = simContext == null || simContext.getSimBaseData() == null
                ? 0 : simContext.getSimBaseData().getAllLevel();
        ConditionUpdate update = config.evaluate(
                new StateConditionEvent(StateConditionEvent.Type.PLAYER_LEVEL, 0, allLevel, 0));
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), allLevel);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(1);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
