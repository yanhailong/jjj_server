package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * @author 11
 * @date 2026/5/15
 */
@Document
public class SimPlayerGameData {
    @Id
    private long playerId;
    //赌场信息
    private Map<Integer, CasinoData> casinoDataMap;
    //当前所在赌场id
    private int currentCasinoId;
    //体力值
    private int stamina;
    //能量
    private int power;
    //研究点 研究点类型：1.普通 2.珍惜  -> 数量
    private Map<Integer, Integer> researchPointMap;
    //是否已完成新手引导
    private boolean guide;
    //上次离线时间 (ms), 用于长/短时掉线判定
    private long lastOfflineTime;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, CasinoData> getCasinoDataMap() {
        return casinoDataMap;
    }

    public void setCasinoDataMap(Map<Integer, CasinoData> casinoDataMap) {
        this.casinoDataMap = casinoDataMap;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public Map<Integer, Integer> getResearchPointMap() {
        return researchPointMap;
    }

    public void setResearchPointMap(Map<Integer, Integer> researchPointMap) {
        this.researchPointMap = researchPointMap;
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

    /**
     * 查询研究点
     *
     * @param skillId
     * @return
     */
    public int findResearchPoint(int skillId) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return 0;
        }
        return this.researchPointMap.get(skillId);
    }

    /**
     * 扣除研究点
     *
     * @param skillId
     * @return
     */
    public boolean deductResearchPoint(int skillId, int points) {
        if (this.researchPointMap == null || this.researchPointMap.isEmpty()) {
            return false;
        }

        Integer beforePoints = this.researchPointMap.get(skillId);
        if (beforePoints == null || beforePoints < points) {
            return false;
        }

        int afterPoints = beforePoints - points;
        if (afterPoints < 1) {
            this.researchPointMap.remove(skillId);
        } else {
            this.researchPointMap.put(skillId, afterPoints);
        }

        return true;
    }
}
