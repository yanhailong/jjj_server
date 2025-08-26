package com.jjg.game.hall.casino.data;

import com.jjg.game.hall.utils.HallTool;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lm
 * @date 2025/8/16 17:00
 */
public class CasinoMachineInfo {
    //机台id
    private long id;
    //BuildingFunction配置表id
    private int configId;
    //BuildingFunction配置表上一级id
    private int lastConfigId;
    //机台建造升级结束时间
    private long buildLvUpEndTime;
    //机台建造升级开始时间
    private long buildLvUpStartTime;
    //收益开始时间
    private long profitStartTime;
    //雇员信息 索引id 雇员信息
    Map<Integer, CasinoEmployment> employmentMap;
    //雇员停止时的收益在重新雇佣雇员的时候设置
    private long lastProfit;

    public long getLastProfit() {
        return lastProfit;
    }

    public void setLastProfit(long lastProfit) {
        this.lastProfit = lastProfit;
    }

    public int getLastConfigId() {
        return lastConfigId;
    }

    public void setLastConfigId(int lastConfigId) {
        this.lastConfigId = lastConfigId;
    }

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

    public void setConfigId(int configId) {
        this.configId = configId;
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

    public int getRealConfigId(long timeMillis) {
        if (buildLvUpEndTime > timeMillis) {
            return lastConfigId;
        } else {
            return configId;
        }
    }

    public long getRunEmploymentNum(long timeMillis) {
        return Objects.isNull(employmentMap) ? 0 : employmentMap.values().stream()
                .filter(employment -> employment.getEmploymentEndTime() > timeMillis)
                .count();
    }

    public static CasinoMachineInfo getNewMachineInfo(BuildingFunctionCfg cfg) {
        CasinoMachineInfo casinoMachineInfo = new CasinoMachineInfo();
        if (cfg.getNumEmployees() > 0) {
            casinoMachineInfo.employmentMap = new ConcurrentHashMap<>();
        }
        casinoMachineInfo.lastConfigId = cfg.getId();
        casinoMachineInfo.configId = cfg.getId();
        casinoMachineInfo.id = HallTool.getNextId();
        return casinoMachineInfo;
    }

    @Override
    public String toString() {
        return "CasinoMachineShowInfo{" +
                "id=" + id +
                ", configId=" + configId +
                ", lastConfigId=" + lastConfigId +
                ", buildLvUpEndTime=" + buildLvUpEndTime +
                ", buildLvUpStartTime=" + buildLvUpStartTime +
                ", profitStartTime=" + profitStartTime +
                ", employmentMap=" + employmentMap +
                '}';
    }
}
