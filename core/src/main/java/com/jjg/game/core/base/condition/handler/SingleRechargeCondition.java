package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
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

/** 11001：单次充值达到金额和渠道要求的累计次数。 */
@Component
public class SingleRechargeCondition extends BaseRedisCondition<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    protected SingleRechargeCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "singleRecharge";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.RECHARGE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(11001, args));
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
        if (!(ctx.event() instanceof PlayerRechargeEvent event)) {
            return match(ctx, config);
        }
        BigDecimal current = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (current.longValue() >= config.target()) {
            return MatchResultData.match();
        }
        ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.recharge(event));
        if (!update.matched() || update.value() <= 0) {
            return MatchResultData.notMatch(getErrorCode(), config.target(), current.longValue());
        }
        BigDecimal total = countDao.incrBy(ctx.player().getId(), getFeatureId(ctx), getCustomId(ctx),
                BigDecimal.valueOf(update.value()));
        return total.longValue() >= config.target()
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), total.longValue());
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11001);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
