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
    //特殊游客列表下次定时刷新时间 (ms)，0 表示不按时段刷新
    private long specialGuestNextRefreshTime;
    //特殊游客当前时段手动刷新次数
    private int specialGuestRefreshCount;
    //当前付费特殊游客配置ID
    private List<Integer> specialGuestPaidCfgIds;
    //本次刷新各付费游客的购买次数 (生成配置ID -> 次数)
    private Map<Integer, Integer> specialGuestPurchaseCounts;
    //付费展示轮次，每次定时或手动刷新递增，用于区分异步到账订单所属列表
    private long specialGuestOfferVersion;
    //当前场景持有的特殊游客数量 (游客道具ID -> 数量)
    private Map<Integer, Long> specialGuestItemCounts;
    //已到账的现金购买订单，用于防止充值重试重复发放
    private Set<String> receivedSpecialGuestOrderIds;
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
    //近期按建筑规划的交互时间戳（用于细分看板实时人数和满意度）— 运行时, 不持久化
    @Transient
    private transient Map<Integer, Deque<Long>> recentBuildingInteractionTimes;

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
        PurchasedGuestData remove = this.purchasedGuestMap.remove(uid);
        if(this.purchasedGuestMap.isEmpty()){
            this.purchasedGuestMap = null;
        }
        return remove;
    }

    public long getSpecialGuestNextRefreshTime() {
        return specialGuestNextRefreshTime;
    }

    public void setSpecialGuestNextRefreshTime(long specialGuestNextRefreshTime) {
        this.specialGuestNextRefreshTime = specialGuestNextRefreshTime;
    }

    public Map<Integer, Integer> getSpecialGuestPurchaseCounts() {
        if (specialGuestPurchaseCounts == null) {
            specialGuestPurchaseCounts = new HashMap<>();
        }
        return specialGuestPurchaseCounts;
    }

    public long getSpecialGuestOfferVersion() {
        return specialGuestOfferVersion;
    }

    public void setSpecialGuestOfferVersion(long specialGuestOfferVersion) {
        this.specialGuestOfferVersion = specialGuestOfferVersion;
    }

    public int getSpecialGuestRefreshCount() {
        return specialGuestRefreshCount;
    }

    public void setSpecialGuestRefreshCount(int specialGuestRefreshCount) {
        this.specialGuestRefreshCount = specialGuestRefreshCount;
    }

    public List<Integer> getSpecialGuestPaidCfgIds() {
        return specialGuestPaidCfgIds;
    }

    public void setSpecialGuestPaidCfgIds(List<Integer> specialGuestPaidCfgIds) {
        this.specialGuestPaidCfgIds = specialGuestPaidCfgIds;
    }

    public Map<Integer, Long> getSpecialGuestItemCounts() {
        if (specialGuestItemCounts == null) {
            specialGuestItemCounts = new HashMap<>();
        }
        return specialGuestItemCounts;
    }

    /**
     * 增加当前场景持有的特殊游客。
     */
    public void addSpecialGuest(int itemId, long count) {
        if (itemId > 0 && count > 0) {
            getSpecialGuestItemCounts().merge(itemId, count, Math::addExact);
        }
    }

    /**
     * 扣除当前场景持有的特殊游客；先整体校验，任一数量不足都不改变数据。
     */
    public boolean consumeSpecialGuests(Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        for (Map.Entry<Integer, Long> entry : items.entrySet()) {
            if (entry.getKey() == null || entry.getKey() <= 0
                    || entry.getValue() == null || entry.getValue() <= 0
                    || getSpecialGuestItemCounts().getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                return false;
            }
        }
        for (Map.Entry<Integer, Long> entry : items.entrySet()) {
            long remain = getSpecialGuestItemCounts().get(entry.getKey()) - entry.getValue();
            if (remain == 0) {
                getSpecialGuestItemCounts().remove(entry.getKey());
            } else {
                getSpecialGuestItemCounts().put(entry.getKey(), remain);
            }
        }
        return true;
    }

    /**
     * 现金订单幂等到账；重复订单视为成功但不重复增加游客。
     */
    public boolean receiveSpecialGuestOrder(String orderId, int itemId, long count) {
        if (orderId == null || orderId.isEmpty() || itemId <= 0 || count <= 0) {
            return false;
        }
        if (receivedSpecialGuestOrderIds == null) {
            receivedSpecialGuestOrderIds = new HashSet<>();
        }
        if (receivedSpecialGuestOrderIds.contains(orderId)) {
            return true;
        }
        addSpecialGuest(itemId, count);
        receivedSpecialGuestOrderIds.add(orderId);
        return true;
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
     * 记录本次游客行程中的建筑交互。服务器在生成游客时已确定完整行程，
     * 因此看板与游客生成使用同一份服务数据，不依赖客户端回报。
     */
    public void recordBuildingInteractions(long now, long windowMs, Collection<Integer> buildingIds) {
        if (buildingIds == null || buildingIds.isEmpty()) {
            return;
        }
        if (this.recentBuildingInteractionTimes == null) {
            this.recentBuildingInteractionTimes = new HashMap<>();
        }
        for (Integer buildingId : buildingIds) {
            if (buildingId == null || buildingId <= 0) {
                continue;
            }
            Deque<Long> times = this.recentBuildingInteractionTimes.computeIfAbsent(
                    buildingId, ignored -> new ArrayDeque<>());
            times.addLast(now);
            removeExpired(times, now - windowMs);
        }
    }

    /**
     * 统计固定窗口内全部建筑交互次数。
     */
    public int countInteractionsInWindow(long now, long windowMs) {
        if (this.recentBuildingInteractionTimes == null || this.recentBuildingInteractionTimes.isEmpty()) {
            return 0;
        }
        int total = 0;
        long cutoff = now - windowMs;
        Iterator<Map.Entry<Integer, Deque<Long>>> iterator = this.recentBuildingInteractionTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Deque<Long>> entry = iterator.next();
            removeExpired(entry.getValue(), cutoff);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            } else {
                total += entry.getValue().size();
            }
        }
        return total;
    }

    /**
     * 统计固定窗口内指定建筑的交互次数。
     */
    public int countBuildingInteractionsInWindow(int buildingId, long now, long windowMs) {
        if (this.recentBuildingInteractionTimes == null) {
            return 0;
        }
        Deque<Long> times = this.recentBuildingInteractionTimes.get(buildingId);
        if (times == null) {
            return 0;
        }
        removeExpired(times, now - windowMs);
        if (times.isEmpty()) {
            this.recentBuildingInteractionTimes.remove(buildingId);
            return 0;
        }
        return times.size();
    }

    private static void removeExpired(Deque<Long> times, long cutoff) {
        while (times != null && !times.isEmpty() && times.peekFirst() < cutoff) {
            times.removeFirst();
        }
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
