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

/** 11004：功能开启后的个人累计充值。 */
@Component
public class TotalRechargeCondition extends BaseRedisCondition<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    protected TotalRechargeCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "totalRecharge";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.RECHARGE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(11004, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        return count.compareTo(BigDecimal.valueOf(config.target())) >= 0
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof PlayerRechargeEvent event) || event.getAmount() == null) {
            return match(ctx, config);
        }
        BigDecimal current = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (current.compareTo(BigDecimal.valueOf(config.target())) >= 0) {
            return MatchResultData.match();
        }
        ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.recharge(event));
        if (!update.matched() || update.value() <= 0) {
            return MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), current);
        }
        BigDecimal total = countDao.incrBy(getFeatureId(ctx), getCustomId(ctx), event.getAmount());
        return total.compareTo(BigDecimal.valueOf(config.target())) >= 0
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), total);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11004);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
