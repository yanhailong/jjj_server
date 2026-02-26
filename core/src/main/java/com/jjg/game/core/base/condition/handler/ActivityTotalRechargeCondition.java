package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerRecharge;
import com.jjg.game.core.base.condition.event.TimeEvent;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11002 活动总充值 要求金额_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class ActivityTotalRechargeCondition implements ConditionHandler<PlayerRecharge> {

    private final PlayerRechargeFlowDao playerRechargeFlowDao;

    public ActivityTotalRechargeCondition(PlayerRechargeFlowDao playerRechargeFlowDao) {
        this.playerRechargeFlowDao = playerRechargeFlowDao;
    }


    @Override
    public String type() {
        return "activityTotalRecharge";
    }

    @Override
    public PlayerRecharge parse(List<String> args) {
        String amount = args.getFirst();
        String channel = args.get(1);
        return new PlayerRecharge(0, Integer.parseInt(channel), new BigDecimal(amount));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerRecharge config) {
        if (ctx.event() instanceof TimeEvent event) {
            long startTime = event.getStartTime();
            long endTime = event.getEndTime();
            BigDecimal total = playerRechargeFlowDao.sumAmountByPlayerIdAndTimeRange(
                    ctx.player().getId(), config.channelId(), startTime, endTime);
            if (total.compareTo(config.amount()) >= 0) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), config.amount(), total);
        }
        return MatchResultData.notMatch(getErrorCode(), config.amount(), BigDecimal.ZERO);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerRecharge config) {
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

