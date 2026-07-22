package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.TimeEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.RechargeConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11002 活动总充值 渠道ID(0=默认所有)_要求金额
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class ActivityTotalRechargeCondition implements ConditionHandler<PreparedCondition> {

    private final PlayerRechargeFlowDao playerRechargeFlowDao;
    private final ConditionRuleRegistry conditionRules;

    public ActivityTotalRechargeCondition(PlayerRechargeFlowDao playerRechargeFlowDao,
                                           ConditionRuleRegistry conditionRules) {
        this.playerRechargeFlowDao = playerRechargeFlowDao;
        this.conditionRules = conditionRules;
    }


    @Override
    public String type() {
        return "activityTotalRecharge";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.RECHARGE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(11002, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        if (ctx.event() instanceof TimeEvent event) {
            long startTime = event.getStartTime();
            long endTime = event.getEndTime();
            int channelId = config.spec().intParameter(0);
            BigDecimal total = playerRechargeFlowDao.sumAmountByPlayerIdAndTimeRange(
                    ctx.player().getId(), channelId, startTime, endTime);
            long amount = LegacyConditionEventAdapter.toNonNegativeLong(total);
            ConditionUpdate update = config.evaluate(new RechargeConditionEvent(channelId, amount));
            if (update.completed(update.apply(0))) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), total);
        }
        return MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), BigDecimal.ZERO);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11002);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}

