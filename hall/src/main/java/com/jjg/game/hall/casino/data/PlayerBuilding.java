package com.jjg.game.hall.casino.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/16 16:06
 */
@Document
public class PlayerBuilding {
    //玩家id
    @Id
    private Long playerId;
    //楼层信息 赌场id->BuildingFloorId -> 机台id
    private Map<Integer, Map<Integer, List<Long>>> buildingData;
    //楼层打扫信息 赌场id-> BuildingFloorId -> 打扫结束时间
    private Map<Integer, Map<Integer, Long>> buildingCleaningEndTime;
    //机台详细信息 机台id->建筑信息
    private Map<Long, MachineInfo> machineInfoData;
    //一键领取结束时间 -1为永久 赌场id-> 结束时间
    private Map<Integer, Long> oneClickClaimEndTimeMap;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, Map<Integer, Long>> getBuildingCleaningEndTime() {
        return buildingCleaningEndTime;
    }

    public void setBuildingCleaningEndTime(Map<Integer, Map<Integer, Long>> buildingCleaningEndTime) {
        this.buildingCleaningEndTime = buildingCleaningEndTime;
    }


    public Map<Integer, Map<Integer, List<Long>>> getBuildingData() {
        return buildingData;
    }

    public void setBuildingData(Map<Integer, Map<Integer, List<Long>>> buildingData) {
        this.buildingData = buildingData;
    }

    public Map<Long, MachineInfo> getMachineInfoData() {
        return machineInfoData;
    }

    public void setMachineInfoData(Map<Long, MachineInfo> machineInfoData) {
        this.machineInfoData = machineInfoData;
    }

    public Map<Integer, Long> getOneClickClaimEndTimeMap() {
        return oneClickClaimEndTimeMap;
    }

    public void setOneClickClaimEndTimeMap(Map<Integer, Long> oneClickClaimEndTimeMap) {
        this.oneClickClaimEndTimeMap = oneClickClaimEndTimeMap;
    }
}
