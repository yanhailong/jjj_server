package com.jjg.game.hall.pointsaward.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 积分大奖玩家数据记录
 */
@Document(collection = "PointsAwardData")
public class PointsAwardData {

    /**
     * 玩家id
     */
    @Id
    private long playerId;

    /**
     * 玩家当前累计的积分
     */
    private long points;

    /**
     * 累计签到天数
     */
    private int signInCount;

    /**
     * 上次签到时间
     */
    private long lastSignInTime;

    /**
     * 首次获取积分时间
     */
    private long firstGetPointsTime;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public int getSignInCount() {
        return signInCount;
    }

    public void setSignInCount(int signInCount) {
        this.signInCount = signInCount;
    }

    public long getLastSignInTime() {
        return lastSignInTime;
    }

    public void setLastSignInTime(long lastSignInTime) {
        this.lastSignInTime = lastSignInTime;
    }
}
