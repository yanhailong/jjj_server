package com.jjg.game.core.data;

import java.util.Objects;

/**
 * 房间类玩家对象 roomPlayer不要存Player对象，因为player对象维护不一定是最新的
 *
 * @author 11
 * @date 2025/6/25 9:28
 */
public class RoomPlayer {
    //玩家座位号
    protected int sit;
    //玩家数据信息
    protected long playerId;
    //当前在线情况
    protected boolean online;
    // 玩家是否机器人
    protected boolean isRobot;
    public RoomPlayer() {
    }

    public RoomPlayer(byte sit, long playerId) {
        this.sit = sit;
        this.playerId = playerId;
    }

    public int getSit() {
        return sit;
    }

    public void setSit(int sit) {
        this.sit = sit;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RoomPlayer that = (RoomPlayer) o;
        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playerId);
    }

    public boolean isRobot() {
        return isRobot;
    }

    public void setRobot(boolean robot) {
        isRobot = robot;
    }
}
