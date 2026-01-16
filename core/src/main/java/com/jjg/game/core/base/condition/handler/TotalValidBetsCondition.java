package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerEffective;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 12007_总累计达到设定有效下注数量（不计算开房间游戏）
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class TotalValidBetsCondition extends BaseRedisCondition<PlayerEffective> {

    protected TotalValidBetsCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "totalValidBets";
    }

    public boolean matchCheck(BetEvent e, PlayerEffective config) {
        return e.getRoomType() < 10;
    }

    @Override
    public PlayerEffective parse(List<String> args) {
        String totalNum = args.getFirst();
        return new PlayerEffective(List.of(), Long.parseLong(totalNum));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerEffective config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (count.longValue() >= config.achievedProcess()) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.achievedProcess(), count.longValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerEffective config) {
        if (ctx.getEvent() instanceof BetEvent e && matchCheck(e, config)) {
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.longValue() >= config.achievedProcess()) {
                return MatchResultData.match();
            }
            BigDecimal add = countDao.incrBy(featureId, customId, BigDecimal.valueOf(e.getBetAmount()));
            if (add.longValue() >= config.achievedProcess()) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), config.achievedProcess(), count.longValue());
        }
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12007);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}
