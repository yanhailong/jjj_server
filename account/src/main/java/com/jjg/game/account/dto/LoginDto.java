package com.jjg.game.account.dto;

/**
 * @author 11
 * @date 2025/9/12 10:26
 */
public class LoginDto {
    //登录类型
    private int loginType;
    //数据
    private String data;
    //mac地址
    private String mac;
    //渠道
    private int channel;
    //设备类型  1.安卓  2.ios
    private int device;
    //手机型号
    private String phoneType;
    //分享码
    private String shareId;
    //子渠道
    private String subChannel;

    public int getLoginType() {
        return loginType;
    }

    public void setLoginType(int loginType) {
        this.loginType = loginType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getSubChannel() {
        return subChannel;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }
}
