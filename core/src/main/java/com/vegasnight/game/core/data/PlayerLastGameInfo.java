package com.vegasnight.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 存储玩家最近一次玩游戏的信息
 * @author 11
 * @date 2025/5/26 17:00
 */
@Document
public class PlayerLastGameInfo {
    @Id
    private long playerId;
    //游戏类型
    private int gameType;
    //游戏id
    private int gameId;
    //房间id
    private int roomId;
    //节点信息
    private String nodePath;
    //是否能退出
    private boolean canExit;
    //最近一次登录时间
    private long lastLoginTime;
    //最近一次离线时间
    private long lastLogoutTime;
    //额外信息
    private String extra;
    //中途掉线
    private boolean halfwayOffline;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public boolean isCanExit() {
        return canExit;
    }

    public void setCanExit(boolean canExit) {
        this.canExit = canExit;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public long getLastLogoutTime() {
        return lastLogoutTime;
    }

    public void setLastLogoutTime(long lastLogoutTime) {
        this.lastLogoutTime = lastLogoutTime;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public boolean isHalfwayOffline() {
        return halfwayOffline;
    }

    public void setHalfwayOffline(boolean halfwayOffline) {
        this.halfwayOffline = halfwayOffline;
    }
}
