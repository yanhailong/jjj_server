package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.RechargeConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 11005：账号生命周期内累计充值；累计数据由全局充值计数器提供。 */
@Component
public class AccountTotalRechargeCondition implements ConditionHandler<PreparedCondition> {
    private final CountDao countDao;
    private final ConditionRuleRegistry conditionRules;

    public AccountTotalRechargeCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        this.countDao = countDao;
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "accountTotalRecharge";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.RECHARGE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(11005, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(CountDao.CountType.RECHARGE.getParam(),
                String.valueOf(ctx.player().getId()));
        ConditionUpdate update = config.evaluate(
                new RechargeConditionEvent(0, LegacyConditionEventAdapter.toNonNegativeLong(count)));
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11005);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
