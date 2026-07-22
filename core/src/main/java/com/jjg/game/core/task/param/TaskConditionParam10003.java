package com.jjg.game.core.task.param;

import com.jjg.game.core.base.condition.numeric.GameConditionEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏实际赢钱参数
 */
public class TaskConditionParam10003 extends DefaultTaskConditionParam {

    /**
     * 游戏id
     */
    private int gameId;

    /**
     * 货币id
     */
    private int coinId;

    /** 本次游戏总押注，用于 10003 的最低押注门槛判断。 */
    private long betAmount;
    private GameConditionEvent conditionEvent;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
        conditionEvent = null;
    }

    public int getCoinId() {
        return coinId;
    }

    public void setCoinId(int coinId) {
        this.coinId = coinId;
        conditionEvent = null;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
        conditionEvent = null;
    }

    @Override
    public void setAddValue(long addValue) {
        super.setAddValue(addValue);
        conditionEvent = null;
    }

    /** 每次业务触发只构造一个不可变事实事件，供同条件下的多个任务配置复用。 */
    public GameConditionEvent conditionEvent() {
        if (conditionEvent == null) {
            conditionEvent = new GameConditionEvent(
                    gameId, gameId, 0, coinId, coinId, betAmount, addValue, 0,
                    betAmount > 0, true, 0, 0, 0, Set.of(), List.of(), Map.of());
        }
        return conditionEvent;
    }
}
