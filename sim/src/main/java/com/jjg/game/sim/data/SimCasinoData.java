package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 赌场信息
 *
 * @author 11
 * @date 2026/5/21
 */
@Document
public class SimCasinoData extends AbstractData {
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
    //知名度 (赌场宣传度)
    private int awareness;
    //能量值 (上限/速率受休息区等级控制, 每赌场独立)
    private int power;
    //研究点 (类型 -> 数量; 类型: 1.普通 2.珍惜)
    private Map<Integer, Integer> researchPointMap;
    //建筑数据
    private Map<Integer, BuildingData> buildingData;
    //拥有的游客 VisitorQuest表
    private Map<Integer, GuestData> guestMap;
    //上次生成游客时间(ms) — 运行时, 不持久化
    @Transient
    private transient long lastGenerateTime;
    //上次在线产出结算时间(ms) — 运行时, 不持久化
    @Transient
    private transient long lastOutputTime;
    //近期生成游客时间戳队列 (用于"10 分钟内生成人数"计算) — 运行时, 不持久化
    @Transient
    private transient Deque<Long> recentGenerateTimes;

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

    /**
     * 添加建筑
     */
    public void putBuilding(BuildingData data) {
        if (this.buildingData == null) {
            this.buildingData = new HashMap<>();
        }
        this.buildingData.put(data.getId(), data);
    }

    /**
     * 查询建筑数据
     */
    public BuildingData findBuilding(int buildingId) {
        if (this.buildingData == null || this.buildingData.isEmpty()) {
            return null;
        }
        return this.buildingData.get(buildingId);
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

    public long getLastOutputTime() {
        return lastOutputTime;
    }

    public void setLastOutputTime(long lastOutputTime) {
        this.lastOutputTime = lastOutputTime;
    }

    /**
     * 增加能量值 (休息区产出)
     */
    public void addPower(long delta) {
        long v = this.power + delta;
        if (v > Integer.MAX_VALUE) {
            v = Integer.MAX_VALUE;
        }
        if (v < 0) {
            v = 0;
        }
        this.power = (int) v;
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
     * 记录一次生成时刻; 同时丢弃窗口外的旧记录
     *
     * @param now      当前时间 (ms)
     * @param windowMs 统计窗口长度 (ms)
     */
    public void recordGenerate(long now, long windowMs) {
        if (this.recentGenerateTimes == null) {
            this.recentGenerateTimes = new ArrayDeque<>();
        }
        this.recentGenerateTimes.addLast(now);
        long cutoff = now - windowMs;
        Iterator<Long> it = this.recentGenerateTimes.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            } else {
                break;
            }
        }
    }

    /**
     * 统计窗口内的生成人数 (会顺带清理过期记录)
     */
    public int countGenerateInWindow(long now, long windowMs) {
        if (this.recentGenerateTimes == null || this.recentGenerateTimes.isEmpty()) {
            return 0;
        }
        long cutoff = now - windowMs;
        Iterator<Long> it = this.recentGenerateTimes.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            } else {
                break;
            }
        }
        return this.recentGenerateTimes.size();
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
