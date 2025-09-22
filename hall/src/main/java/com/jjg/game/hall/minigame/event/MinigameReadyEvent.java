package com.jjg.game.hall.minigame.event;

/**
 * 小游戏配置初始化完成的准备事件
 */
public class MinigameReadyEvent {

    /**
     * 游戏id
     */
    private int gameId;

    public MinigameReadyEvent() {

    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
