package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 玩家的账号信息
 * @author 11
 * @date 2025/5/24 17:54
 */
@Document
public class Account {
    @Id
    private long playerId;
    //游客登录账号
    private String guest;
    //手机号
    private String phoneNumber;
    //邮箱账号
    private String email;
    //0.游客  1.实名用户
    private int accountType;
    //账号创建时间
    private int createTime;
    //注册时的mac
    private String registerMac;
    //最近一次登录的mac
    private String lastLoginMac;
    //当前状态
    private int status;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getGuest() {
        return guest;
    }

    public void setGuest(String guest) {
        this.guest = guest;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
        this.accountType = accountType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getLastLoginMac() {
        return lastLoginMac;
    }

    public void setLastLoginMac(String lastLoginMac) {
        this.lastLoginMac = lastLoginMac;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
