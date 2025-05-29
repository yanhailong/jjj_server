package com.vegasnight.game.core.data;

/**
 * 玩家对象
 * @author 11
 * @date 2025/5/26 11:18
 */
public class Player {
    //玩家id
    private long id;
    private int roomId;
    private String ip;
    //设备类型
    private int deviceType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }
}
