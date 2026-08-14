package com.jjg.game.sim.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingUnlockEquipmentData {
    //建筑id
    private int buildId;
    //BuildingUpgradeTable表中该建筑，配置了UnlockEquipment参数的，最大的等级
    private int maxLevel;
    //等级对应的解锁设备
    private Map<Integer, List<Integer>> levelEquipmentMap;

    public int getBuildId() {
        return buildId;
    }

    public void setBuildId(int buildId) {
        this.buildId = buildId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public Map<Integer, List<Integer>> getLevelEquipmentMap() {
        return levelEquipmentMap;
    }

    public void setLevelEquipmentMap(Map<Integer, List<Integer>> levelEquipmentMap) {
        this.levelEquipmentMap = levelEquipmentMap;
    }

    public List<Integer> getLevelUnlockEquipment(int level) {
        if(this.levelEquipmentMap == null || this.levelEquipmentMap.isEmpty()){
            return null;
        }
        return this.levelEquipmentMap.get(level);
    }

    public void setLevelUnlockEquipment(int level, List<Integer> list) {
        if(this.levelEquipmentMap == null){
            this.levelEquipmentMap = new HashMap<>();
        }
        this.levelEquipmentMap.put(level, list);
    }
}
