package com.jjg.game.core.base.condition.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerBet;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 10001_游戏ID(0 = 任意游戏)（不区分倍场)_大于等于总押注条件_等于目标投注次数
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class BetFrequencyCondition extends BaseRedisCondition<PlayerBet> {

    public BetFrequencyCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "betFrequency";
    }

    public boolean matchCheck(BetEvent e, PlayerBet config) {
        return (config.gameType() == 0 || config.gameType() == e.getGameType()) && CollectionUtil.isNotEmpty(e.getBetList());
    }

    @Override
    public PlayerBet parse(List<String> args) {
        String gameType = args.getFirst();
        String needBet = args.get(1);
        String times = args.get(2);
        return new PlayerBet(Integer.parseInt(gameType), Integer.parseInt(needBet), Integer.parseInt(times), 0, 0);
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerBet config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (count.intValue() >= config.times()) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.times(), count.intValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerBet config) {
        if (ctx.event() instanceof BetEvent e && matchCheck(e, config)) {
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.intValue() >= config.times()) {
                return MatchResultData.match();
            }
            List<Integer> betList = e.getBetList();
            long addCount = betList.stream()
                    .filter(i -> i >= config.achievedProcess())
                    .count();
            if (addCount > 0) {
                BigDecimal add = countDao.incrBy(ctx.player().getId(), featureId, customId, BigDecimal.valueOf(addCount));
                if (add.intValue() >= config.times()) {
                    return MatchResultData.match();
                }
            }
            return MatchResultData.notMatch(getErrorCode(), config.times(), count.intValue());
        }
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(10001);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }

}
