package com.jjg.game.account.dto;

/**
 * @author 11
 * @date 2025/10/14 15:51
 */
public class ServerUrlDto {
    //登录类型
    private long playerId;
    //mac地址
    private String mac;
    //渠道
    private int channel;
    //设备类型  1.安卓  2.ios
    private int device;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getDevice() {
        return device;
    }

    public void setDevice(int device) {
        this.device = device;
    }
}
