package com.jjg.game.sim.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.season.data.SeasonPlayerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * 单玩家模拟经营会话上下文
 *
 * @author 11
 * @date 2026/5/26
 */
public class SimPlayerContext {
    private static final Logger log = LoggerFactory.getLogger(SimPlayerContext.class);

    private long playerId;
    private PlayerController playerController;
    private SimBaseData simBaseData;
    //技能 gameType -> data
    private Map<Integer, SimSkillsData> skillsDataMap = new HashMap<>();
    //雇员 employeeId -> data (玩家级, 跨场景共享)
    private Map<Integer, SimEmployeeData> employeeMap = new HashMap<>();
    //当前所在场景 (内存中仅保留当前场景, 切换时落库旧场景并加载新场景)
    private SimCasinoData currentCasino;
    //已解锁场景 (研究院等级 场景id->等级); 登录缓存, 仅供本玩家高频读取, 解锁新场景时同步刷新
    private SimCasinoUnlock casinoUnlock;

    //待领取的离线收益 (上线计算, 领取后清空)
    private SimOfflineReward pendingOffline;

    //主线/成就任务数据
    private SimTaskData simTaskData;
    //多人协作任务数据
    private SimCoopTaskData simCoopTaskData;
    //赛季玩法玩家聚合数据
    private SeasonPlayerData seasonPlayerData;
    //勋章品质加成缓存 (condition表id -> 千分比加成值; 登录/成就领奖后刷新; 内存态不落库, 供收益计算零IO读取)
    private Map<Integer, Integer> medalBuffMap = new HashMap<>();

    //近期已处理的旋转 RPC 幂等 id (内存态; 防 slots 超时重试双计, 同玩家 RPC 串行执行无需加锁)
    private final ArrayDeque<Long> recentSpinIds = new ArrayDeque<>();
    private static final int RECENT_SPIN_ID_MAX = 16;

    //上次落库检查时间 (ms; 业务置 0 可强制下个 tick 立即检查落库)
    private long lastSaveTime;
    //上次活跃时间
    private long lastActiveTime;
    //上次随机拜访时间 (ms, 内存态, 服务端兜底限频用)
    private long lastRandomVisitTime;
    //上次联盟加速抵扣检查时间 (ms, 内存态; tick 内按玩家节流 Redis 访问)
    private long lastSpeedupCheckTime;

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public SimBaseData getSimBaseData() {
        return simBaseData;
    }

    public void setSimBaseData(SimBaseData simBaseData) {
        this.simBaseData = simBaseData;
    }

    public Map<Integer, SimSkillsData> getSkillsDataMap() {
        return skillsDataMap;
    }

    public void setSkillsDataMap(Map<Integer, SimSkillsData> skillsDataMap) {
        this.skillsDataMap = skillsDataMap;
    }

    public Map<Integer, SimEmployeeData> getEmployeeMap() {
        return employeeMap;
    }

    public void setEmployeeMap(Map<Integer, SimEmployeeData> employeeMap) {
        this.employeeMap = employeeMap == null ? new HashMap<>() : employeeMap;
    }

    public SimEmployeeData getEmployee(int employeeId) {
        return this.employeeMap.get(employeeId);
    }

    public void putEmployee(SimEmployeeData data) {
        this.employeeMap.put(data.getEmployeeId(), data);
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long playerId() {
        return this.playerId;
    }

    public void send(Object msg) {
        playerController.send(msg);
    }

    // ---------------------------------------------------------------------
    // 场景数据访问 (内存仅保留当前场景)
    // ---------------------------------------------------------------------

    /**
     * 获取当前所在场景
     */
    public SimCasinoData getCurrentCasino() {
        return currentCasino;
    }

    /**
     * 设置当前场景实体 (由 SimCasinoService 在加载/切换时维护)
     */
    public void setCurrentCasino(SimCasinoData currentCasino) {
        this.currentCasino = currentCasino;
    }

    /**
     * 切换当前场景 id (内存场景实体替换由 SimCasinoService 完成)
     */
    public void switchCasino(int casinoId) {
        this.simBaseData.setCurrentCasinoId(casinoId);
    }

    public SimCasinoUnlock getCasinoUnlock() {
        return casinoUnlock;
    }

    public void setCasinoUnlock(SimCasinoUnlock casinoUnlock) {
        this.casinoUnlock = casinoUnlock;
    }

    public SimOfflineReward getPendingOffline() {
        return pendingOffline;
    }

    public void setPendingOffline(SimOfflineReward pendingOffline) {
        this.pendingOffline = pendingOffline;
    }

    public SimTaskData getSimTaskData() {
        return simTaskData;
    }

    public void setSimTaskData(SimTaskData simTaskData) {
        this.simTaskData = simTaskData;
    }

    public SimCoopTaskData getSimCoopTaskData() {
        return simCoopTaskData;
    }

    public void setSimCoopTaskData(SimCoopTaskData simCoopTaskData) {
        this.simCoopTaskData = simCoopTaskData;
    }

    public SeasonPlayerData getSeasonPlayerData() {
        return seasonPlayerData;
    }

    public void setSeasonPlayerData(SeasonPlayerData seasonPlayerData) {
        this.seasonPlayerData = seasonPlayerData;
    }

    public Map<Integer, Integer> getMedalBuffMap() {
        return medalBuffMap;
    }

    public void setMedalBuffMap(Map<Integer, Integer> medalBuffMap) {
        this.medalBuffMap = medalBuffMap == null ? new HashMap<>() : medalBuffMap;
    }

    /**
     * 标记一次旋转 RPC 已处理。
     *
     * @return false 表示该 spinId 近期已处理过 (slots 超时重试的重复投递), 调用方应跳过联动
     */
    public boolean markSpinProcessed(long spinId) {
        if (recentSpinIds.contains(spinId)) {
            return false;
        }
        recentSpinIds.addLast(spinId);
        if (recentSpinIds.size() > RECENT_SPIN_ID_MAX) {
            recentSpinIds.removeFirst();
        }
        return true;
    }

    public long getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLastSaveTime(long lastSaveTime) {
        this.lastSaveTime = lastSaveTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public long getLastRandomVisitTime() {
        return lastRandomVisitTime;
    }

    public void setLastRandomVisitTime(long lastRandomVisitTime) {
        this.lastRandomVisitTime = lastRandomVisitTime;
    }

    public long getLastSpeedupCheckTime() {
        return lastSpeedupCheckTime;
    }

    public void setLastSpeedupCheckTime(long lastSpeedupCheckTime) {
        this.lastSpeedupCheckTime = lastSpeedupCheckTime;
    }

    // ---------------------------------------------------------------------
    // 调试
    // ---------------------------------------------------------------------

    public void printGuest() {
        SimCasinoData casino = getCurrentCasino();
        if (casino == null) {
            return;
        }
        Map<Integer, GuestData> guestMap = casino.getGuestMap();
        if (guestMap == null || guestMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, GuestData> en : guestMap.entrySet()) {
            System.out.println(JSONObject.toJSONString(en.getValue()));
        }
    }

    public void printBuilding() {
        SimCasinoData casino = getCurrentCasino();
        if (casino == null) {
            return;
        }
        Map<Integer, BuildingData> buildingMap = casino.getBuildingData();
        if (buildingMap == null || buildingMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, BuildingData> en : buildingMap.entrySet()) {
            System.out.println(JSONObject.toJSONString(en.getValue()));
        }
    }

    public SimSkillsData getSkillData(int gameType) {
        if (this.skillsDataMap == null || this.skillsDataMap.isEmpty()) {
            return null;
        }
        return this.skillsDataMap.get(gameType);
    }

    public void addSkillData(SimSkillsData data) {
        if (this.skillsDataMap == null || this.skillsDataMap.isEmpty()) {
            this.skillsDataMap = new HashMap<>();
        }
        this.skillsDataMap.put(data.getGameType(), data);
    }
}
