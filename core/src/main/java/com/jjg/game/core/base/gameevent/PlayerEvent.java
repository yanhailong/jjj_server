package com.jjg.game.core.base.gameevent;

import com.jjg.game.core.data.Player;

/**
 * 玩家事件
 *
 * @author 2CL
 */
public class PlayerEvent extends GameEvent {

    private final Player player;

    public PlayerEvent(Player player, EGameEventType gameEventType, Object eventChangeValue, Object newlyValue) {
        super(gameEventType, eventChangeValue, newlyValue);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
