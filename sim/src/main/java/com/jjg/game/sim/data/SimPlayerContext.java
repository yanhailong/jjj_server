package com.jjg.game.sim.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    //雇员 employeeId -> data (玩家级, 跨赌场共享)
    private Map<Integer, SimEmployeeData> employeeMap = new HashMap<>();
    //当前所在赌场 (内存中仅保留当前赌场, 切换时落库旧赌场并加载新赌场)
    private SimCasinoData currentCasino;

    //待领取的离线收益 (上线计算, 领取后清空)
    private SimOfflineReward pendingOffline;

    //上次落库时间 (ms)
    private long lastSaveTime;
    //上次活跃时间
    private long lastActiveTime;

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
    // 赌场数据访问 (内存仅保留当前赌场)
    // ---------------------------------------------------------------------

    /**
     * 获取当前所在赌场
     */
    public SimCasinoData getCurrentCasino() {
        return currentCasino;
    }

    /**
     * 设置当前赌场实体 (由 SimCasinoService 在加载/切换时维护)
     */
    public void setCurrentCasino(SimCasinoData currentCasino) {
        this.currentCasino = currentCasino;
    }

    /**
     * 切换当前赌场 id (内存赌场实体替换由 SimCasinoService 完成)
     */
    public void switchCasino(int casinoId) {
        this.simBaseData.setCurrentCasinoId(casinoId);
    }

    public SimOfflineReward getPendingOffline() {
        return pendingOffline;
    }

    public void setPendingOffline(SimOfflineReward pendingOffline) {
        this.pendingOffline = pendingOffline;
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
