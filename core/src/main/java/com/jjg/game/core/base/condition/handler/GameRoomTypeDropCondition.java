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
 * 12006_每累计达到有效流水时触发|指定倍场游戏类型可多个倍场
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class GameRoomTypeDropCondition extends BaseEffectiveCondition {


    public GameRoomTypeDropCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "gameRoomTypeDrop";
    }

    @Override
    public boolean matchCheck(BetEvent e, PlayerEffective config) {
        return config.ids().contains(e.getRoomType()) || config.ids().getFirst() == 0;
    }

    @Override
    public PlayerEffective parse(List<String> args) {
        return baseParse(args);
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
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12006);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}
