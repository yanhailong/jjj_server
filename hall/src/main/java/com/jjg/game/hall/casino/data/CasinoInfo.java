package com.jjg.game.hall.casino.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赌场信息
 *
 * @author lm
 * @date 2025/8/20 09:56
 */
public class CasinoInfo {
    private int id;
    //楼层信息BuildingFloorId -> 机台id
    private Map<Integer, List<Long>> buildingData;
    //楼层打扫信息  BuildingFloorId -> 打扫结束时间
    private Map<Integer, Long> buildingCleaningEndTime;
    //机台详细信息 机台id->建筑信息
    private Map<Long, MachineInfo> machineInfoData;
    //一键领取结束时间 -1为永久 赌场id-> 结束时间
    private long oneClickClaimEndTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<Integer, List<Long>> getBuildingData() {
        return buildingData;
    }

    public void setBuildingData(Map<Integer, List<Long>> buildingData) {
        this.buildingData = buildingData;
    }

    public Map<Integer, Long> getBuildingCleaningEndTime() {
        return buildingCleaningEndTime;
    }

    public void setBuildingCleaningEndTime(Map<Integer, Long> buildingCleaningEndTime) {
        this.buildingCleaningEndTime = buildingCleaningEndTime;
    }

    public Map<Long, MachineInfo> getMachineInfoData() {
        return machineInfoData;
    }

    public void setMachineInfoData(Map<Long, MachineInfo> machineInfoData) {
        this.machineInfoData = machineInfoData;
    }

    public long getOneClickClaimEndTime() {
        return oneClickClaimEndTime;
    }

    public void setOneClickClaimEndTime(long oneClickClaimEndTime) {
        this.oneClickClaimEndTime = oneClickClaimEndTime;
    }

    public static CasinoInfo getNewCasinoInfo(int casinoId) {
        CasinoInfo casinoInfo = new CasinoInfo();
        casinoInfo.buildingCleaningEndTime = new HashMap<>();
        casinoInfo.buildingData = new HashMap<>();
        casinoInfo.machineInfoData = new HashMap<>();
        casinoInfo.id = casinoId;
        return casinoInfo;
    }
}
