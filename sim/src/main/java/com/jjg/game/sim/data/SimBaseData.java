package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家 sim 基础数据
 *
 * @author 11
 * @date 2026/5/15
 */
@Document
public class SimBaseData extends AbstractData {
    @Id
    private long playerId;
    //体力值
    private int stamina;
    //当前所在赌场id
    private int currentCasinoId;
    //是否已完成新手引导
    private boolean guide;
    //上次离线时间 (ms), 用于长/短时掉线判定
    private long lastOfflineTime;
    //能量值
    private int power;
    //每日掉落次数 (dropItemId -> 当日已掉次数)
    private Map<Integer, Integer> dailyDropCount;
    //每日掉落计数重置日 (yyyyMMdd)
    private int dropResetDay;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public boolean isGuide() {
        return guide;
    }

    public void setGuide(boolean guide) {
        this.guide = guide;
    }

    public int getCurrentCasinoId() {
        return currentCasinoId;
    }

    public void setCurrentCasinoId(int currentCasinoId) {
        this.currentCasinoId = currentCasinoId;
    }

    public long getLastOfflineTime() {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(long lastOfflineTime) {
        this.lastOfflineTime = lastOfflineTime;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public Map<Integer, Integer> getDailyDropCount() {
        return dailyDropCount;
    }

    public void setDailyDropCount(Map<Integer, Integer> dailyDropCount) {
        this.dailyDropCount = dailyDropCount;
    }

    public int getDropResetDay() {
        return dropResetDay;
    }

    public void setDropResetDay(int dropResetDay) {
        this.dropResetDay = dropResetDay;
    }

    /**
     * 跨天则重置每日掉落计数
     *
     * @param today yyyyMMdd
     */
    public void checkResetDropCount(int today) {
        if (this.dropResetDay != today) {
            this.dropResetDay = today;
            if (this.dailyDropCount != null) {
                this.dailyDropCount.clear();
            }
        }
    }

    /**
     * 查询某 dropItem 当日已掉落次数
     */
    public int getDropCount(int dropItemId) {
        if (this.dailyDropCount == null || this.dailyDropCount.isEmpty()) {
            return 0;
        }
        return this.dailyDropCount.getOrDefault(dropItemId, 0);
    }

    /**
     * 某 dropItem 当日掉落次数 +1
     */
    public void addDropCount(int dropItemId) {
        if (this.dailyDropCount == null) {
            this.dailyDropCount = new HashMap<>();
        }
        this.dailyDropCount.merge(dropItemId, 1, Integer::sum);
    }
}
