package com.jjg.game.slots.data;

import com.jjg.game.core.data.PlayerController;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 11
 * @date 2025/7/11 10:51
 */
public class SlotsPlayerGameData {
    protected PlayerController playerController;
    //游戏类型
    protected int gameType;
    //场次配置id
    protected int wareId;
    //是否玩过该slots游戏
    protected AtomicBoolean hasPlaySlots = new AtomicBoolean(false);

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getWareId() {
        return wareId;
    }

    public void setWareId(int wareId) {
        this.wareId = wareId;
    }

    public AtomicBoolean getHasPlaySlots() {
        return hasPlaySlots;
    }

    public void setHasPlaySlots(AtomicBoolean hasPlaySlots) {
        this.hasPlaySlots = hasPlaySlots;
    }

    public long playerId(){
        return playerController.playerId();
    }
}
