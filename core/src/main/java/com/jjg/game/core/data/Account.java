package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家的账号信息
 *
 * @author 11
 * @date 2025/5/24 17:54
 */
@Document
public class Account {
    @Id
    private long playerId;
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
    //最近一次登录时间
    private long lastLoginTime;
    //最近一次离线时间
    private long lastOfflineTime;
    //当前状态
    private int status;
    //渠道
    private ChannelType channel;
    //第三方账号
    private Map<LoginType, String> thirdAccounts;


    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public long getLastOfflineTime() {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(long lastOfflineTime) {
        this.lastOfflineTime = lastOfflineTime;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
        this.accountType = accountType;
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

    public ChannelType getChannel() {
        return channel;
    }

    public void setChannel(ChannelType channel) {
        this.channel = channel;
    }

    public Map<LoginType, String> getThirdAccounts() {
        return thirdAccounts;
    }

    public void setThirdAccounts(Map<LoginType, String> thirdAccounts) {
        this.thirdAccounts = thirdAccounts;
    }

    public boolean addThirdAccount(LoginType loginType, String accountName) {
        if(thirdAccounts == null){
            thirdAccounts = new HashMap<>();
        }

        if(thirdAccounts.containsKey(loginType)){
           return false;
        }
        thirdAccounts.put(loginType, accountName);
        return true;
    }

    public String getThirdAccount(LoginType loginType) {
        if(thirdAccounts == null){
            return null;
        }
        return thirdAccounts.get(loginType);
    }
}
