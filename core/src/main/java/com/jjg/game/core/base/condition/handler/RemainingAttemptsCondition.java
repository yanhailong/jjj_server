package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.RemainingAttempts;
import com.jjg.game.core.base.condition.event.RemainingAttemptsEvent;
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
public class RemainingAttemptsCondition extends BaseRedisCondition<RemainingAttempts> {


    protected RemainingAttemptsCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "remainingAttempts";
    }

    public boolean matchCheck(RemainingAttemptsEvent event, RemainingAttempts config) {
        return event.activityId() == config.activityId();
    }

    @Override
    public RemainingAttempts parse(List<String> args) {
        String activityId = args.getFirst();
        String needTimes = args.get(1);
        String maxTimes = args.get(1);
        return new RemainingAttempts(Long.parseLong(activityId), Integer.parseInt(needTimes), Integer.parseInt(maxTimes));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, RemainingAttempts config) {
        //参与次数
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (config.maxTimes() - count.intValue() >= config.remainTimes()) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, RemainingAttempts config) {
        if (ctx.event() instanceof RemainingAttemptsEvent event && matchCheck(event, config)) {
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (config.maxTimes() - count.intValue() >= config.remainTimes()) {
                return MatchResultData.match();
            }
            BigDecimal add = countDao.incrBy(ctx.player().getId(), featureId, customId, BigDecimal.valueOf(event.addTimes()));
            if (config.maxTimes() - add.intValue() >= config.remainTimes()) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode());
        }
        return match(ctx, config);
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
