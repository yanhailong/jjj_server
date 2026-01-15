package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerEffective;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author lm
 * @date 2026/1/14 14:32
 */
public abstract class BaseEffectiveCondition extends BaseRedisCondition<PlayerEffective> {

    public BaseEffectiveCondition(CountDao countDao) {
        super(countDao);
    }

    public abstract boolean matchCheck(BetEvent e, PlayerEffective config);

    public PlayerEffective baseParse(List<String> args) {
        String needCount = args.getFirst();
        List<Integer> list = args.subList(1, args.size()).stream().map(Integer::parseInt).toList();
        return new PlayerEffective(list, Long.parseLong(needCount));
    }

    public MatchResultData baseMatch(ConditionContext ctx, PlayerEffective config) {
        Object event = ctx.getEvent();
        if (event instanceof BetEvent e && matchCheck(e, config)) {
            String featureId = getFeatureId(ctx);
            String customId = getCustomId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            BigDecimal totalCount = count.add(BigDecimal.valueOf(e.getBetAmount()));
            BigDecimal times = totalCount.divide(BigDecimal.valueOf(config.achievedProcess()), RoundingMode.DOWN);
            if (times.compareTo(BigDecimal.ONE) >= 0) {
                countDao.incrBy(featureId, customId, totalCount.subtract(times.multiply(BigDecimal.valueOf(config.achievedProcess()))));
                return new MatchResultData(MatchResult.MATCH, times.intValue(), Code.SUCCESS, BigDecimal.valueOf(config.achievedProcess()), BigDecimal.valueOf(totalCount.longValue()));
            }
            return MatchResultData.notMatch(getErrorCode());
        }
        return MatchResultData.unknown();
    }
}
