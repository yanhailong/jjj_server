package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2026/1/19
 */
public class VerCode {
    private long playerId;
    private int code;
    //限制频繁的过期时间
    private int idleTime;
    //验证码类型
    private VerCodeType verCodeType;
    private String data;

    public VerCode() {
    }

    public VerCode(long playerId) {
        this.playerId = playerId;
    }

    public VerCode(String data) {
        this.data = data;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public VerCodeType getVerCodeType() {
        return verCodeType;
    }

    public void setVerCodeType(VerCodeType verCodeType) {
        this.verCodeType = verCodeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
