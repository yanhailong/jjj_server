package com.jjg.game.gm.vo;

/**
 * @author 11
 * @date 2025/8/25 16:38
 */
public class PlayerVo {
    private long playerId;
    private String nickName;
    private long gold;
    private long diamond;
    private int vipLevel;
    private String ip;
    private int createTime;
    private String registerMac;
    private int isBan;
    private int isOffline;
    private String mobile;
    private SafeVo safeVo;
    private int level;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getGold() {
        return gold;
    }

    public void setGold(long gold) {
        this.gold = gold;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public String getRegisterMac() {
        return registerMac;
    }

    public void setRegisterMac(String registerMac) {
        this.registerMac = registerMac;
    }

    public int getIsBan() {
        return isBan;
    }

    public void setIsBan(int isBan) {
        this.isBan = isBan;
    }

    public int getIsOffline() {
        return isOffline;
    }

    public void setIsOffline(int isOffline) {
        this.isOffline = isOffline;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public SafeVo getSafeInfo() {
        return safeVo;
    }

    public void setSafeInfo(SafeVo safeVo) {
        this.safeVo = safeVo;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
