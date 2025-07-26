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
    protected int roomCfgId;
    //是否玩过该slots游戏
    protected AtomicBoolean hasPlaySlots = new AtomicBoolean(false);
    //当前所处状态(美元快递) 0.正常  1.二选一  2.正在免费旋转
    protected int status;
    //原始押注值
    private long lastStake;

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

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLastStake() {
        return lastStake;
    }

    public void setLastStake(long lastStake) {
        this.lastStake = lastStake;
    }
}
