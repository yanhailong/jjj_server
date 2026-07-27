package com.jjg.game.sim.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * 场景信息
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
    //场景id (业务 id, 配合 CasinoListCfg)
    private int casinoId;
    //经验
    private int exp;
    //场景等级 (CasinoStatsSheet.level)
    private int casinoLevel;
    //当前繁荣度
    private int prosperity;
    //知名度 (场景宣传度)
    private int awareness;
    //建筑数据
    private Map<Integer, BuildingData> buildingData;
    //拥有的游客 VisitorQuest表
    private Map<Integer, GuestData> guestMap;
    //已生成待领奖的购买游客 (uid -> data, 落库用于断线重连)
    private Map<String, PurchasedGuestData> purchasedGuestMap;
    //主管id   employeeProfileConfig.ProfessionID -> employeeId
    private Map<Integer, Integer> managerEmployMap;
    //游客羁绊
    private Set<Integer> guestBondsSet;
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

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getCasinoLevel() {
        return casinoLevel;
    }

    public void setCasinoLevel(int casinoLevel) {
        this.casinoLevel = casinoLevel;
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

    public Map<Integer, Integer> getManagerEmployMap() {
        return managerEmployMap;
    }

    public void setManagerEmployMap(Map<Integer, Integer> managerEmployMap) {
        this.managerEmployMap = managerEmployMap;
    }

    public Set<Integer> getGuestBondsSet() {
        return guestBondsSet;
    }

    public void setGuestBondsSet(Set<Integer> guestBondsSet) {
        this.guestBondsSet = guestBondsSet;
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

    public Map<String, PurchasedGuestData> getPurchasedGuestMap() {
        return purchasedGuestMap;
    }

    public void setPurchasedGuestMap(Map<String, PurchasedGuestData> purchasedGuestMap) {
        this.purchasedGuestMap = purchasedGuestMap;
    }

    /**
     * 添加待领奖的购买游客
     */
    public void addPurchasedGuest(PurchasedGuestData data) {
        if (this.purchasedGuestMap == null) {
            this.purchasedGuestMap = new HashMap<>();
        }
        this.purchasedGuestMap.put(data.getUid(), data);
    }

    /**
     * 按 uid 查询购买游客
     */
    public PurchasedGuestData findPurchasedGuest(String uid) {
        if (this.purchasedGuestMap == null || this.purchasedGuestMap.isEmpty()) {
            return null;
        }
        return this.purchasedGuestMap.get(uid);
    }

    /**
     * 领奖后移除购买游客
     */
    public PurchasedGuestData removePurchasedGuest(String uid) {
        if (this.purchasedGuestMap == null || this.purchasedGuestMap.isEmpty()) {
            return null;
        }
        return this.purchasedGuestMap.remove(uid);
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

    public int manageEmploy(int professionId) {
        if (this.managerEmployMap == null || this.managerEmployMap.isEmpty()) {
            return 0;
        }
        return this.managerEmployMap.getOrDefault(professionId, 0);
    }

    public void addManagerEmploy(int professionId, int employId) {
        if (this.managerEmployMap == null || this.managerEmployMap.isEmpty()) {
            this.managerEmployMap = new HashMap<>();
        }
        this.managerEmployMap.put(professionId, employId);
    }

    public boolean containsGuestBonds(int bondsId) {
        if (this.guestBondsSet == null || this.guestBondsSet.isEmpty()) {
            return false;
        }
        return this.guestBondsSet.contains(bondsId);
    }

    public void addGuestBonds(int bondsId) {
        if (this.guestBondsSet == null || this.guestBondsSet.isEmpty()) {
            this.guestBondsSet = new HashSet<>();
        }
        this.guestBondsSet.add(bondsId);
    }


    public boolean employIsManager(int employeeId) {
        if (this.managerEmployMap == null || this.managerEmployMap.isEmpty()) {
            return false;
        }
        return this.managerEmployMap.entrySet().stream().anyMatch(e -> e.getValue() == employeeId);
    }
}
