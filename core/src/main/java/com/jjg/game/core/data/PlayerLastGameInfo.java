package com.jjg.game.core.data;

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
    //游戏id
    private int gameUniqueId;
    //游戏类型
    private int gameType;
    //场次id
    private int wareId;
    //房间id
    private long roomId;
    //节点信息
    private String nodePath;
    //中途掉线
    private boolean halfwayOffline;
    //额外信息
    private String extra;
    // 房间配置ID
    private int roomCfgId;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getGameUniqueId() {
        return gameUniqueId;
    }

    public void setGameUniqueId(int gameUniqueId) {
        this.gameUniqueId = gameUniqueId;
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

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public boolean isHalfwayOffline() {
        return halfwayOffline;
    }

    public void setHalfwayOffline(boolean halfwayOffline) {
        this.halfwayOffline = halfwayOffline;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }
}
