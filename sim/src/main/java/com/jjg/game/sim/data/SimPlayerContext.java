package com.jjg.game.sim.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 单玩家模拟经营会话上下文
 * <p>
 * 仅作为数据载体 + 通信通道, 业务逻辑放到对应 Service 中。
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
    //当前赌场引用缓存 (由 setPlayerGameData / switchCasino 维护)
    private CasinoData currentCasino;

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
    }

    /**
     * 根据 playerGameData.currentCasinoId 刷新当前赌场引用
     */
    public void refreshCurrentCasino() {
        if (this.playerGameData == null) {
            this.currentCasino = null;
            return;
        }
        Map<Integer, CasinoData> map = this.playerGameData.getCasinoDataMap();
        this.currentCasino = map == null ? null : map.get(this.playerGameData.getCurrentCasinoId());
    }

    /**
     * 调试: 打印当前赌场全部游客数据
     */
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

    /**
     * 调试: 打印当前赌场全部建筑数据
     */
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
