package com.jjg.game.core.data;


/**
 * @author 11
 * @date 2026/1/19
 */
public class SmsConfigInfo {
    private int cfgId;
    private String sendSmsUrl;
    private String appId;
    private String appKey;
    private String appSecret;
    private boolean open;

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public String getSendSmsUrl() {
        return sendSmsUrl;
    }

    public void setSendSmsUrl(String sendSmsUrl) {
        this.sendSmsUrl = sendSmsUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
