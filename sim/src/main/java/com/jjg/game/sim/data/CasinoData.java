package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 赌场信息
 *
 * @author 11
 * @date 2026/5/21
 */
@Document
public class CasinoData {
    //联合主键 playerId:casinoId
    @Id
    private String id;
    //玩家id
    @Indexed
    private long playerId;
    //赌场id (业务 id, 配合 CasinoListCfg)
    private int casinoId;
    //等级id
    private int statsId;
    //当前繁荣度
    private int prosperity;
    //曝光结束时间(ms)，0 表示未曝光
    private long exposureEndTime;
    //知名度
    private int awareness;
    //能量值 (上限/速率受休息区等级控制, 每赌场独立)
    private int power;
    //研究点 (类型 -> 数量; 类型: 1.普通 2.珍惜)
    //备注: 研究院本身是一个建筑, 其等级走 buildingData.get(研发部 id).level
    private Map<Integer, Integer> researchPointMap;
    //建筑数据
    private Map<Integer, BuildingData> buildingData;
    //拥有的游客 VisitorQuest表
    private Map<Integer, GuestData> guestMap;
    //上次生成游客时间(ms) — 运行时, 不持久化
    @Transient
    private transient long lastGenerateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getCasinoId() {
        return casinoId;
    }

    public void setCasinoId(int casinoId) {
        this.casinoId = casinoId;
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

    /**
     * 根据 playerId 和 casinoId 构建联合主键
     */
    public void buildKey() {
        this.id = buildKey(this.playerId, this.casinoId);
    }

    public static String buildKey(long playerId, int casinoId) {
        return playerId + ":" + casinoId;
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
