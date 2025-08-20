package com.jjg.game.hall.casino.data;

import com.jjg.game.hall.utils.HallTool;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/16 17:00
 */
public class MachineInfo {
    //机台id
    private long id;
    //BuildingFunction配置表id
    private int configId;
    //机台建造升级结束时间
    private long buildLvUpEndTime;
    //机台建造升级开始时间
    private long buildLvUpStartTime;
    //收益开始时间
    private long profitStartTime;
    //雇员信息 索引id 雇员信息
    Map<Integer, CasinoEmployment> employmentMap;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<Integer, CasinoEmployment> getEmploymentMap() {
        return employmentMap;
    }

    public void setEmploymentMap(Map<Integer, CasinoEmployment> employmentMap) {
        this.employmentMap = employmentMap;
    }

    public int getConfigId() {
        return configId;
    }

    public long getProfitStartTime() {
        return profitStartTime;
    }

    public void setProfitStartTime(long profitStartTime) {
        this.profitStartTime = profitStartTime;
    }

    public long getBuildLvUpEndTime() {
        return buildLvUpEndTime;
    }

    public void setBuildLvUpEndTime(long buildLvUpEndTime) {
        this.buildLvUpEndTime = buildLvUpEndTime;
    }

    public long getBuildLvUpStartTime() {
        return buildLvUpStartTime;
    }

    public void setBuildLvUpStartTime(long buildLvUpStartTime) {
        this.buildLvUpStartTime = buildLvUpStartTime;
    }

    public static MachineInfo getNewMachineInfo(int casinoId, BuildingFunctionCfg cfg) {
        MachineInfo machineInfo = new MachineInfo();
        if (cfg.getNumEmployees() > 0) {
            machineInfo.employmentMap = new HashMap<>();
        }
        machineInfo.configId = cfg.getId();
        machineInfo.id = HallTool.getNextId();
        return machineInfo;
    }
}
