package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/6/10 9:48
 */
public class PlayerSessionToken {
    private long playerId;
    private String token;
    private int loginType;
    //过期时间
    private long expireTime;
    private int channel;
    private String ip;
    //设备类型  1.安卓  2.ios
    private int device;
    private String mac;
    private int registerChannel;
    private String sharId;
    private String subChannel;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getLoginType() {
        return loginType;
    }

    public void setLoginType(int loginType) {
        this.loginType = loginType;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getDevice() {
        return device;
    }

    public void setDevice(int device) {
        this.device = device;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getRegisterChannel() {
        return registerChannel;
    }

    public void setRegisterChannel(int registerChannel) {
        this.registerChannel = registerChannel;
    }

    public String getSharId() {
        return sharId;
    }

    public void setSharId(String sharId) {
        this.sharId = sharId;
    }

    public String getSubChannel() {
        return subChannel;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }
}
