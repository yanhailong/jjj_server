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
    //研究点 (类型 -> 数量; 类型: 1.普通 2.珍惜)
    private Map<Integer, Integer> researchPointMap;

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

    public Map<Integer, Integer> getResearchPointMap() {
        return researchPointMap;
    }

    public void setResearchPointMap(Map<Integer, Integer> researchPointMap) {
        this.researchPointMap = researchPointMap;
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

    public void addResearchPoint(int type,int num){
        if(this.researchPointMap == null){
            this.researchPointMap = new HashMap<>();
        }
        this.researchPointMap.merge(type,num,Integer::sum);
    }

    /**
     * 查询某类型研究点的当前数量
     */
    public int findResearchPoint(int type) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return 0;
        }
        Integer v = this.researchPointMap.get(type);
        return v == null ? 0 : v;
    }

    /**
     * 扣除研究点 (内部已做余额校验); 余额不足返回 false
     */
    public boolean deductResearchPoint(int type, int points) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return false;
        }
        Integer before = this.researchPointMap.get(type);
        if (before == null || before < points) {
            return false;
        }
        int after = before - points;
        if (after < 1) {
            this.researchPointMap.remove(type);
        } else {
            this.researchPointMap.put(type, after);
        }
        return true;
    }
}
