package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerBet;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 10003_游戏ID(0 = 任意游戏)（不区分倍场）_大于等于总押注条件_大于等于目标获胜金额_货币ID(金币或钻石)
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class PlayGameWinMoneyCondition implements ConditionHandler<PlayerBet> {

    @Override
    public String type() {
        return "playGameWinMoney";
    }

    public boolean matchCheck(BetEvent e, PlayerBet config) {
        return (config.gameType() == 0 || config.gameType() == e.getGameType())
                && e.getItemId() == config.itemId();
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerBet config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(10003);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }

    @Override
    public PlayerBet parse(List<String> args) {
        String gameType = args.getFirst();
        String needBet = args.get(1);
        String times = args.get(2);
        String win = args.get(3);
        String itemId = args.get(4);
        return new PlayerBet(Integer.parseInt(gameType), Integer.parseInt(needBet), Integer.parseInt(times), Integer.parseInt(itemId), Integer.parseInt(win));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerBet config) {
        if (ctx.event() instanceof BetEvent e && matchCheck(e, config)) {
                if (e.getBetAmount() >= config.achievedProcess() && e.getWinAmount() >= config.winCount()) {
                    return MatchResultData.match();
                } else {
                    return MatchResultData.notMatch(getErrorCode());
                }
        }
        return MatchResultData.unknown();
    }
}
