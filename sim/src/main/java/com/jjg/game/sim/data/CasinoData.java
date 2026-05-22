package com.jjg.game.sim.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 赌场信息
 *
 * @author 11
 * @date 2026/5/21
 */
public class CasinoData {
    //赌场id
    private int id;
    //等级id
    private int statsId;
    //当前繁荣度
    private int prosperity;
    //曝光结束时间(ms)，0 表示未曝光
    private long exposureEndTime;
    //研究院等级
    private int researchId;
    //知名度
    private int awareness;
    //建筑数据
    private Map<Integer, BuildingData> buildingData;
    //拥有的游客 VisitorQuest表
    private Map<Integer, GuestData> guestMap;
    //上次生成游客时间(ms)
    private long lastGenerateTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatsId() {
        return statsId;
    }

    public void setStatsId(int statsId) {
        this.statsId = statsId;
    }

    public int getProsperity() {
        return prosperity;
    }

    public void setProsperity(int prosperity) {
        this.prosperity = prosperity;
    }

    public long getExposureEndTime() {
        return exposureEndTime;
    }

    public void setExposureEndTime(long exposureEndTime) {
        this.exposureEndTime = exposureEndTime;
    }

    public int getResearchId() {
        return researchId;
    }

    public void setResearchId(int researchId) {
        this.researchId = researchId;
    }

    public int getAwareness() {
        return awareness;
    }

    public void setAwareness(int awareness) {
        this.awareness = awareness;
    }

    public Map<Integer, BuildingData> getBuildingData() {
        return buildingData;
    }

    public void setBuildingData(Map<Integer, BuildingData> buildingData) {
        this.buildingData = buildingData;
    }

    public Map<Integer, GuestData> getGuestMap() {
        return guestMap;
    }

    public void setGuestMap(Map<Integer, GuestData> guestMap) {
        this.guestMap = guestMap;
    }

    public long getLastGenerateTime() {
        return lastGenerateTime;
    }

    public void setLastGenerateTime(long lastGenerateTime) {
        this.lastGenerateTime = lastGenerateTime;
    }

    /**
     * 根据游客id找到guestData
     *
     * @param guestId
     * @return
     */
    public GuestData findGuestData(int guestId) {
        if (this.guestMap == null || this.guestMap.isEmpty()) {
            return null;
        }
        return this.guestMap.get(guestId);
    }

    /**
     * 添加游客信息
     *
     * @param guestData
     */
    public void addGuest(GuestData guestData) {
        if (this.guestMap == null) {
            this.guestMap = new HashMap<>();
        }
        this.guestMap.put(guestData.getId(), guestData);
    }
}
