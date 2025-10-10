package com.jjg.game.core.task.param;

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

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getCoinId() {
        return coinId;
    }

    public void setCoinId(int coinId) {
        this.coinId = coinId;
    }
}
