package com.jjg.game.sim.data;

/**
 * 建筑数据
 *
 * @author 11
 * @date 2026/5/22
 */
public class BuildingData {
    //建筑id (对应 BuildingAreaTableCfg.id)
    private int id;
    //当前等级 (对应 BuildingUpgradeTableCfg.level)
    private int level;
    //升级 CD 结束时间 (ms); 0 表示无 CD
    private long cdEndTime;
    //已发起的清 CD 广告次数; 用于限流
    private int adClearCount;
    //主管id
    private int managerEmployId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getCdEndTime() {
        return cdEndTime;
    }

    public void setCdEndTime(long cdEndTime) {
        this.cdEndTime = cdEndTime;
    }

    public int getAdClearCount() {
        return adClearCount;
    }

    public void setAdClearCount(int adClearCount) {
        this.adClearCount = adClearCount;
    }

    public int getManagerEmployId() {
        return managerEmployId;
    }

    public void setManagerEmployId(int managerEmployId) {
        this.managerEmployId = managerEmployId;
    }

    /**
     * 是否处于升级 CD 中 (尚未到时)
     */
    public boolean isUpgrading(long now) {
        return cdEndTime > now;
    }

    /**
     * CD 是否已到时 (可以执行完成升级)
     */
    public boolean isUpgradeReady(long now) {
        return cdEndTime > 0 && cdEndTime <= now;
    }
}
