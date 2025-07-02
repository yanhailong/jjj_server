package com.jjg.game.common.concurrent.priority;

import com.jjg.game.common.concurrent.BaseHandlerRunnable;

/**
 * 优先级队列handler
 *
 * @author 2CL
 */
public abstract class BasePriorityHandlerRunnable extends BaseHandlerRunnable {
    private PlayerPriority playerPriority;

    public BasePriorityHandlerRunnable(PlayerPriority playerPriority) {
        this.playerPriority = playerPriority;
    }

    public PlayerPriority getPlayerPriority() {
        return playerPriority;
    }

    public void setPlayerPriority(PlayerPriority playerPriority) {
        this.playerPriority = playerPriority;
    }
}
