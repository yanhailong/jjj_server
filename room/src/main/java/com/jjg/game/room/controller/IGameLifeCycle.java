package com.jjg.game.room.controller;

/**
 * 房间内游戏生命周期
 *
 * @author 2CL
 */
public interface IGameLifeCycle extends IRoomLifeCycle {

    /**
     * 自动执行房间中的逻辑片段
     */
    void autoRunGamePhase();

    /**
     * 结算逻辑
     */
    void gameOverSettlement();
}
