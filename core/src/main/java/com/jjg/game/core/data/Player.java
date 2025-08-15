package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

/**
 * 玩家对象
 *
 * @author 11
 * @date 2025/5/26 11:18
 */
@Document
public class Player {
    //玩家id
    private long id;
    //昵称
    private String nickName;
    //性别  0.女  1.男  2.其他
    private byte gender;
    //头像id
    private int headImgId;
    //头像框id
    private int headFrameId;
    //国旗id
    private int nationalId;
    //称号id
    private int titleId;
    //房间id
    private long roomId;
    //游戏类型
    private int gameType;
    //房间配置ID
    private int roomCfgId;
    //金币
    private long gold;
    //钻石
    private long diamond;
    //保险箱金币
    private long safeBoxGold;
    //保险箱钻石
    private long safeBoxDiamond;
    //等级
    private int level;
    //经验
    private long exp;
    //vip等级
    private int vipLevel;
    //vip经验
    private long vipExp;
    //流水
    private long statement;
    //ip地址
    private String ip;
    //设备类型
    private int deviceType;
    //创建时间
    private int createTime;
    //玩家数据更新时间
    private long updateTime;
    //好友房邀请码
    private int friendRoomInvitationCode;

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

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public int getHeadImgId() {
        return headImgId;
    }

    public void setHeadImgId(int headImgId) {
        this.headImgId = headImgId;
    }

    public int getHeadFrameId() {
        return headFrameId;
    }

    public void setHeadFrameId(int headFrameId) {
        this.headFrameId = headFrameId;
    }

    public int getNationalId() {
        return nationalId;
    }

    public void setNationalId(int nationalId) {
        this.nationalId = nationalId;
    }

    public int getTitleId() {
        return titleId;
    }

    public void setTitleId(int titleId) {
        this.titleId = titleId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public long getGold() {
        return gold;
    }

    public void setGold(long gold) {
        if (gold < 0) {
            throw new RuntimeException("给玩家设置负数金币值");
        }
        this.gold = gold;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }

    public long getSafeBoxGold() {
        return safeBoxGold;
    }

    public void setSafeBoxGold(long safeBoxGold) {
        this.safeBoxGold = safeBoxGold;
    }

    public long getSafeBoxDiamond() {
        return safeBoxDiamond;
    }

    public void setSafeBoxDiamond(long safeBoxDiamond) {
        this.safeBoxDiamond = safeBoxDiamond;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public long getVipExp() {
        return vipExp;
    }

    public void setVipExp(long vipExp) {
        this.vipExp = vipExp;
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

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getFriendRoomInvitationCode() {
        return friendRoomInvitationCode;
    }

    public void setFriendRoomInvitationCode(int friendRoomInvitationCode) {
        this.friendRoomInvitationCode = friendRoomInvitationCode;
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
