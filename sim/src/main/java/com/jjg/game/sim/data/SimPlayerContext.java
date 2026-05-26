package com.jjg.game.sim.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 单玩家模拟经营会话上下文
 *
 * @author 11
 * @date 2026/5/26
 */
public class SimPlayerContext {
    private static final Logger log = LoggerFactory.getLogger(SimPlayerContext.class);

    private PlayerController playerController;
    private SimPlayerGameData playerGameData;
    //技能 gameType -> data
    private Map<Integer, SimSkillsData> skillsDataMap;
    //玩家拥有的赌场 (CasinoData 独立 collection, 由 SimManager 在 createContext 时装配)
    private Map<Integer, CasinoData> casinoMap = new HashMap<>();
    //当前赌场引用缓存 (由 setPlayerGameData / switchCasino / setCasinoMap 维护)
    private CasinoData currentCasino;

    //脏标记 (玩家级)
    private boolean dirty;
    //脏赌场 id 集合 (赌场级)
    private final Set<Integer> dirtyCasinoIds = new HashSet<>();
    //上次落库时间 (ms)
    private long lastSaveTime;

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public SimPlayerGameData getPlayerGameData() {
        return playerGameData;
    }

    public void setPlayerGameData(SimPlayerGameData playerGameData) {
        this.playerGameData = playerGameData;
        refreshCurrentCasino();
    }

    public Map<Integer, SimSkillsData> getSkillsDataMap() {
        return skillsDataMap;
    }

    public void setSkillsDataMap(Map<Integer, SimSkillsData> skillsDataMap) {
        this.skillsDataMap = skillsDataMap;
    }

    public SimSkillsData getSkillData(int gameType) {
        if (this.skillsDataMap == null || this.skillsDataMap.isEmpty()) {
            return null;
        }
        return this.skillsDataMap.get(gameType);
    }

    public long playerId() {
        return playerController.playerId();
    }

    public void send(Object msg) {
        playerController.send(msg);
    }

    // ---------------------------------------------------------------------
    // 赌场数据访问
    // ---------------------------------------------------------------------

    public Map<Integer, CasinoData> getCasinoMap() {
        return casinoMap;
    }

    public void setCasinoMap(Map<Integer, CasinoData> casinoMap) {
        this.casinoMap = casinoMap == null ? new HashMap<>() : casinoMap;
        refreshCurrentCasino();
    }

    public CasinoData getCasino(int casinoId) {
        return casinoMap.get(casinoId);
    }

    public void putCasino(CasinoData casino) {
        casinoMap.put(casino.getCasinoId(), casino);
    }

    /**
     * 获取当前所在赌场 (走缓存)
     */
    public CasinoData getCurrentCasino() {
        return currentCasino;
    }

    /**
     * 切换当前赌场
     */
    public void switchCasino(int casinoId) {
        this.playerGameData.setCurrentCasinoId(casinoId);
        refreshCurrentCasino();
        markDirty();
    }

    /**
     * 根据 playerGameData.currentCasinoId 刷新当前赌场引用
     */
    public void refreshCurrentCasino() {
        if (this.playerGameData == null) {
            this.currentCasino = null;
            return;
        }
        this.currentCasino = casinoMap.get(this.playerGameData.getCurrentCasinoId());
    }

    // ---------------------------------------------------------------------
    // 脏标记 (玩家级 + 赌场级)
    // ---------------------------------------------------------------------

    public boolean isDirty() {
        return dirty;
    }

    /**
     * 标脏 (玩家级): 修改 SimPlayerGameData 的字段时调用
     */
    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    /**
     * 标脏 (赌场级): 修改当前赌场的字段时调用
     */
    public void markCasinoDirty() {
        if (this.currentCasino != null) {
            dirtyCasinoIds.add(this.currentCasino.getCasinoId());
        }
    }

    /**
     * 标脏 (赌场级): 指定 casinoId
     */
    public void markCasinoDirty(int casinoId) {
        dirtyCasinoIds.add(casinoId);
    }

    /**
     * 是否有任何赌场被标脏
     */
    public boolean hasDirtyCasino() {
        return !dirtyCasinoIds.isEmpty();
    }

    /**
     * 取出当前脏赌场快照, 同时清空内部集合
     */
    public Set<Integer> consumeDirtyCasinoIds() {
        if (dirtyCasinoIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> snapshot = new HashSet<>(dirtyCasinoIds);
        dirtyCasinoIds.clear();
        return snapshot;
    }

    /**
     * 当前脏赌场集合 (只读快照, 不清空)
     */
    public Collection<Integer> peekDirtyCasinoIds() {
        return Collections.unmodifiableSet(dirtyCasinoIds);
    }

    public long getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLastSaveTime(long lastSaveTime) {
        this.lastSaveTime = lastSaveTime;
    }

    // ---------------------------------------------------------------------
    // 调试
    // ---------------------------------------------------------------------

    public void printGuest() {
        CasinoData casino = getCurrentCasino();
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
        CasinoData casino = getCurrentCasino();
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
}
