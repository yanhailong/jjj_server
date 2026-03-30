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
    //手机型号
    private String phoneType;
    //马甲id
    private int westeId;
    //adid
    private String adid;
    //fcm
    private String fcm;

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

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public int getWesteId() {
        return westeId;
    }

    public void setWesteId(int westeId) {
        this.westeId = westeId;
    }

    public String getAdid() {
        return adid;
    }

    public void setAdid(String adid) {
        this.adid = adid;
    }

    public String getFcm() {
        return fcm;
    }

    public void setFcm(String fcm) {
        this.fcm = fcm;
    }
}
