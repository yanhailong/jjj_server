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
    //最后一次活跃时间
    private int lastActiveTime;
    //是否在线
    private boolean online;
    //最近一次的押注(单线押分)
    private long oneBetScore;
    //最近一次的押注(总押分)
    private long allBetScore;

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

    public int getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(int lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getOneBetScore() {
        return oneBetScore;
    }

    public void setOneBetScore(long oneBetScore) {
        this.oneBetScore = oneBetScore;
    }

    public long getAllBetScore() {
        return allBetScore;
    }

    public void setAllBetScore(long allBetScore) {
        this.allBetScore = allBetScore;
    }
}
