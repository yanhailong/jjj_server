package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.Map;

/** 每玩家每期一条记录；历史记录供跨期支付到账和未领奖励结算使用。 */
@Document
public class ActivePassData extends AbstractData {
    @Id
    private String id;
    private long playerId;
    private int passId;
    private long endTime;
    private long points;
    private int purchasedPoints;
    private int purchasedTracks;
    private int day;
    private boolean settled;
    private Map<Integer, Integer> claimedRewards = new LinkedHashMap<>();
    private Map<Integer, ActivePassTask> dailyTasks = new LinkedHashMap<>();
    private Map<Integer, ActivePassTask> periodTasks = new LinkedHashMap<>();

    public static String key(long playerId, int passId) { return playerId + ":" + passId; }
    @Override public void buildKey() { id = key(playerId, passId); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public int getPassId() { return passId; }
    public void setPassId(int passId) { this.passId = passId; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }
    public int getPurchasedPoints() { return purchasedPoints; }
    public void setPurchasedPoints(int purchasedPoints) { this.purchasedPoints = purchasedPoints; }
    public int getPurchasedTracks() { return purchasedTracks; }
    public void setPurchasedTracks(int purchasedTracks) { this.purchasedTracks = purchasedTracks; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
    public Map<Integer, Integer> getClaimedRewards() { return claimedRewards; }
    public void setClaimedRewards(Map<Integer, Integer> value) { claimedRewards = value; }
    public Map<Integer, ActivePassTask> getDailyTasks() { return dailyTasks; }
    public void setDailyTasks(Map<Integer, ActivePassTask> value) { dailyTasks = value; }
    public Map<Integer, ActivePassTask> getPeriodTasks() { return periodTasks; }
    public void setPeriodTasks(Map<Integer, ActivePassTask> value) { periodTasks = value; }
}
