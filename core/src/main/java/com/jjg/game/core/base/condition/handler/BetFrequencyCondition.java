package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 10001：满足游戏和最低押注要求的投注次数。 */
@Component
public class BetFrequencyCondition extends BaseRedisCondition<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    public BetFrequencyCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "betFrequency";
    }

    @Override
    public EGameEventType eventType() {
        return null;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(10001, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        return count.longValue() >= config.target()
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), count.longValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof BetEvent event)) {
            return match(ctx, config);
        }
        BigDecimal current = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (current.longValue() >= config.target()) {
            return MatchResultData.match();
        }

        long increment = 0;
        List<Integer> bets = event.getBetList();
        if (bets == null || bets.isEmpty()) {
            ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.game(event));
            increment = update.matched() ? Math.max(0, update.value()) : 0;
        } else {
            for (Integer bet : bets) {
                if (bet == null) {
                    continue;
                }
                ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.game(event, bet));
                if (update.matched()) {
                    increment += Math.max(0, update.value());
                }
            }
        }
        if (increment <= 0) {
            return MatchResultData.notMatch(getErrorCode(), config.target(), current.longValue());
        }
        BigDecimal total = countDao.incrBy(ctx.player().getId(), getFeatureId(ctx), getCustomId(ctx),
                BigDecimal.valueOf(increment));
        return total.longValue() >= config.target()
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), total.longValue());
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(10001);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
