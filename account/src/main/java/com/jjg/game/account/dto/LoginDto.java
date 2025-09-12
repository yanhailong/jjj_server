package com.jjg.game.account.dto;

/**
 * @author 11
 * @date 2025/9/12 10:26
 */
public class LoginDto {
    private String mac;
    private int channel;

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
}
