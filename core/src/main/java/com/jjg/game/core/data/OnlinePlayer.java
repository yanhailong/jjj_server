package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/27 16:58
 */
@Document
public class OnlinePlayer {
    @Id
    private long playerId;
    //渠道
    private int channel;
    //游戏
    private int gameType;
    //场次id
    private int roomCfgId;
    //游戏
    private String subChannel;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public String getSubChannel() {
        return subChannel;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }
}
