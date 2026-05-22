package com.jjg.game.sim.data;

/**
 * 游客行程中的一个目的地 (建筑id + 设备id), 是否已交互
 *
 * @author 11
 * @date 2026/5/21
 */
public class Destination {
    //建筑id (InteractionAreasTable.id)
    private int buildingId;
    //设备id (建筑内设备序号, 1..DeviceLimit)
    private int deviceId;
    //是否已完成交互
    private boolean done;

    public Destination() {
    }

    public Destination(int buildingId, int deviceId) {
        this.buildingId = buildingId;
        this.deviceId = deviceId;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
