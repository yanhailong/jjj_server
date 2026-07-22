package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 10003_游戏ID(0 = 任意游戏)（不区分倍场）_大于等于总押注条件_大于等于目标获胜金额_货币ID(金币或钻石)
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class PlayGameWinMoneyCondition implements ConditionHandler<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    public PlayGameWinMoneyCondition(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "playGameWinMoney";
    }

    @Override
    public EGameEventType eventType() {
        return null;
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(10003);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(10003, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof BetEvent event)) {
            return MatchResultData.unknown();
        }
        ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.game(event));
        if (!update.matched()) {
            //旧表达式树把游戏/货币不相关的事件视为 UNKNOWN，让同一表达式中的其他事件条件继续处理。
            return MatchResultData.unknown();
        }
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), update.value());
    }
}
