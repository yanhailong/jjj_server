package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.RemainingAttemptsEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.CountDao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11002_总累计充值大于等于金额_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class RemainingAttemptsCondition extends BaseRedisCondition<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    protected RemainingAttemptsCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "remainingAttempts";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.ACTIVITY_JOIN;
    }

    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(5, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        long remaining = Math.max(0, config.spec().parameter(2) - count.longValue());
        ConditionUpdate update = state(config, config.spec().parameter(0), remaining);
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), remaining);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (ctx.event() instanceof RemainingAttemptsEvent event) {
            ConditionUpdate relevant = state(config, event.activityId(), 0);
            if (!relevant.matched()) {
                return match(ctx, config);
            }
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal add = countDao.incrBy(ctx.player().getId(), featureId, customId, BigDecimal.valueOf(event.addTimes()));
            long remaining = Math.max(0, config.spec().parameter(2) - add.longValue());
            ConditionUpdate update = state(config, event.activityId(), remaining);
            return update.completed(update.apply(0))
                    ? MatchResultData.match()
                    : MatchResultData.notMatch(getErrorCode(), config.target(), remaining);
        }
        return match(ctx, config);
    }

    private static ConditionUpdate state(PreparedCondition config, long activityId, long remaining) {
        return config.evaluate(new StateConditionEvent(StateConditionEvent.Type.REMAINING_ATTEMPTS,
                activityId, remaining, config.spec().parameter(2)));
    }

    @Override
    public void addBaseProgress(long playerId, BigDecimal addValue) {
        countDao.incrBy(playerId, type(), String.valueOf(playerId), addValue);
    }

    @Override
    public int getErrorCode() {
        return 0;
    }
}

