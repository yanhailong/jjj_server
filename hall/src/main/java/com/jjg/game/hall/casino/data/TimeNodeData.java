package com.jjg.game.hall.casino.data;

/**
 * @author lm
 * @date 2025/8/22 14:05
 */
public class TimeNodeData {
    //类型 1.机台 2.雇员
    private int type;
    //配置表id
    private int configId;
    //升级结束时间
    private long endTime;
    //升级开始时间
    private long startTime;
    //上一级
    private int lastLevelConfigId;

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public int getLastLevelConfigId() {
        return lastLevelConfigId;
    }

    public void setLastLevelConfigId(int lastLevelConfigId) {
        this.lastLevelConfigId = lastLevelConfigId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public static TimeNodeData getNewTimeNodeData(CasinoEmployment casinoEmployment) {
        TimeNodeData machineData = new TimeNodeData();
        machineData.type = 2;
        machineData.configId = casinoEmployment.getEmploymentId();
        machineData.startTime = casinoEmployment.getEmploymentStartTime();
        machineData.endTime = casinoEmployment.getEmploymentEndTime();
        return machineData;
    }

    public static TimeNodeData getNewTimeNodeData(CasinoMachineInfo casinoMachineInfo, int lastLevelConfigId) {
        TimeNodeData machineData = new TimeNodeData();
        machineData.type = 1;
        machineData.configId = casinoMachineInfo.getConfigId();
        machineData.startTime = casinoMachineInfo.getBuildLvUpStartTime();
        machineData.endTime = casinoMachineInfo.getBuildLvUpEndTime();
        machineData.lastLevelConfigId = lastLevelConfigId;
        return machineData;
    }
}
