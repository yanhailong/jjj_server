package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 旧活动“累计有效押注后重复达成”语义的数据持久化适配层。
 * 参数校验、事件过滤、进度值和目标值全部来自统一 numeric 规则；本类只保留旧表达式需要的 Redis 余数。
 */
public abstract class BaseEffectiveCondition extends BaseRedisCondition<PreparedCondition> {
    private final int conditionId;
    private final ConditionRuleRegistry conditionRules;

    protected BaseEffectiveCondition(CountDao countDao, ConditionRuleRegistry conditionRules, int conditionId) {
        super(countDao);
        this.conditionRules = conditionRules;
        this.conditionId = conditionId;
    }

    @Override
    public EGameEventType eventType() {
        return null;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return prepare(ConditionSpec.from(conditionId, args));
    }

    protected final PreparedCondition prepare(ConditionSpec spec) {
        return conditionRules.prepare(spec);
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        return MatchResultData.unknown();
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof BetEvent event)) {
            return MatchResultData.unknown();
        }
        ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.game(event));
        if (!update.matched()) {
            return MatchResultData.unknown();
        }

        String featureId = getFeatureId(ctx);
        String customId = getCustomId(ctx);
        BigDecimal current = countDao.getCount(featureId, customId);
        BigDecimal increment = BigDecimal.valueOf(Math.max(0, update.value()));
        BigDecimal total = current.add(increment);
        BigDecimal target = BigDecimal.valueOf(config.target());
        BigDecimal completedTimes = total.divide(target, 0, RoundingMode.DOWN);
        if (completedTimes.compareTo(BigDecimal.ONE) >= 0) {
            //旧表达式允许一次事件跨过多个目标，Redis 中只保留未达成下一次的余数。
            BigDecimal delta = increment.subtract(completedTimes.multiply(target));
            countDao.incrBy(ctx.player().getId(), featureId, customId, delta);
            return new MatchResultData(MatchResult.MATCH, completedTimes.intValue(), Code.SUCCESS, target, total);
        }
        countDao.incrBy(ctx.player().getId(), featureId, customId, increment);
        return MatchResultData.notMatch(getErrorCode());
    }
}
