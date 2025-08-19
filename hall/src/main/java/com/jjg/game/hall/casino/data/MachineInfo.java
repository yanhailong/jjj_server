package com.jjg.game.hall.casino.data;

import java.util.List;
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
    //雇员配置表id
    private int employmentId;
    //雇员开始时间
    private long employmentStartTime;
    //雇员结束时间
    private long employmentEndTime;
    //机台建造升级结束时间
    private long buildLvUpEndTime;
    //机台建造升级开始时间
    private long buildLvUpStartTime;
    //收益开始时间
    private long profitStartTime;
    //雇员信息
    List<CasinoEmployment> employmentList;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<CasinoEmployment> getEmploymentList() {
        return employmentList;
    }

    public void setEmploymentList(List<CasinoEmployment> employmentList) {
        this.employmentList = employmentList;
    }

    public int getConfigId() {
        return configId;
    }

    public long getEmploymentStartTime() {
        return employmentStartTime;
    }

    public void setEmploymentStartTime(long employmentStartTime) {
        this.employmentStartTime = employmentStartTime;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public int getEmploymentId() {
        return employmentId;
    }

    public void setEmploymentId(int employmentId) {
        this.employmentId = employmentId;
    }

    public long getEmploymentEndTime() {
        return employmentEndTime;
    }

    public void setEmploymentEndTime(long employmentEndTime) {
        this.employmentEndTime = employmentEndTime;
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
}
