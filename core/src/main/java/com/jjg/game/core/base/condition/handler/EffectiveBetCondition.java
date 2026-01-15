package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerEffective;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 12002_每累积达到设定有效下注数量（不计算开房间游戏）
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class EffectiveBetCondition extends BaseEffectiveCondition {


    public EffectiveBetCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "effectiveBet";
    }

    @Override
    public boolean matchCheck(BetEvent e, PlayerEffective config) {
        return e.getRoomType() < 10;
    }

    @Override
    public PlayerEffective parse(List<String> args) {
        String needCount = args.getFirst();
        return new PlayerEffective(List.of(), Long.parseLong(needCount));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerEffective config) {
        return MatchResultData.unknown();
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerEffective config) {
        return baseMatch(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12002);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}
