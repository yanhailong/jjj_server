package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

/**
 * 玩家对象
 * @author 11
 * @date 2025/5/26 11:18
 */
@Document
public class Player {
    //玩家id
    private long id;
    //昵称
    private String nickName;
    //房间id
    private int roomId;
    //游戏类型
    private int gameType;
    //场次类型
    private int wareId;
    //金币
    private long gold;
    //钻石
    private long diamond;
    //vip等级
    private int vipLevel;
    //经验
    private long exp;
    //流水
    private long statement;
    //ip地址
    private String ip;
    //设备类型
    private int deviceType;
    //创建时间
    private int createTime;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getWareId() {
        return wareId;
    }

    public void setWareId(int wareId) {
        this.wareId = wareId;
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

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public long getStatement() {
        return statement;
    }

    public void setStatement(long statement) {
        this.statement = statement;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
