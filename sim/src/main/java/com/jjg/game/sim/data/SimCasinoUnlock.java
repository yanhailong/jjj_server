package com.jjg.game.sim.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 每个场景解锁的等级
 *
 * @author 11
 * @date 2026/6/4
 */
public class SimCasinoUnlock {
    private long playerId;
    //研究院等级 场景id->研究院等级
    private Map<Integer, Integer> researchLevelMap;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, Integer> getResearchLevelMap() {
        return researchLevelMap;
    }

    public void setResearchLevelMap(Map<Integer, Integer> researchLevelMap) {
        this.researchLevelMap = researchLevelMap;
    }

    public void changeUnlockLevel(int id, int level) {
        if (this.researchLevelMap == null) {
            this.researchLevelMap = new HashMap<>();
        }
        this.researchLevelMap.put(id, level);
    }
}
