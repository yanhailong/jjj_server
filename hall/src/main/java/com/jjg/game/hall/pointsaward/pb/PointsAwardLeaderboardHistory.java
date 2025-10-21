package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 积分大奖排行历史记录
 */
@ProtobufMessage
@ProtoDesc("积分大奖排行历史记录")
public class PointsAwardLeaderboardHistory {

    /**
     * 玩家id
     */
    @ProtoDesc("玩家id")
    private long playerId;

    /**
     * 名次
     */
    @ProtoDesc("名次")
    private int rank;

    /**
     * 分数
     */
    @ProtoDesc("分数")
    private int rankPoints;

    /**
     * 昵称
     */
    @ProtoDesc("昵称")
    private String nickName;

    /**
     * 性别  0.女  1.男  2.其他
     */
    @ProtoDesc("性别  0.女  1.男  2.其他")
    private byte gender;

    /**
     * 头像id
     */
    @ProtoDesc("头像id")
    private int headImgId;

    /**
     * 头像框id
     */
    @ProtoDesc("头像框id")
    private int headFrameId;

    /**
     * 国旗id
     */
    @ProtoDesc("国旗id")
    private int nationalId;

    /**
     * 称号id
     */
    @ProtoDesc("称号id")
    private int titleId;

    /**
     * 奖励道具
     */
    @ProtoDesc("奖励道具")
    private List<String> reward;

    /**
     * 领奖码
     */
    @ProtoDesc("领奖码")
    private String code;

    /**
     * 奖励价值
     */
    @ProtoDesc("奖励价值")
    private long price;

    /**
     * 图片资源
     */
    @ProtoDesc("图片资源")
    private String picRes;

    /**
     * 奖励类型
     */
    @ProtoDesc("奖励类型")
    private int awardType;

    /**
     * 排行榜类型 1=上午榜 2=下午榜 3=月榜
     */
    @ProtoDesc("排行榜类型")
    private int rankType;

    /**
     * 结束时间 用于计算排行榜名字 未结束的排行榜为当前时间戳
     */
    @ProtoDesc("结束时间 用于计算排行榜名字 未结束的排行榜为当前时间戳")
    private long endTime;

    /**
     * 领奖码的过期时间
     */
    @ProtoDesc("领奖码的过期时间 -1表示已经领取了")
    private long expiredTime;

    public int getRankType() {
        return rankType;
    }

    public void setRankType(int rankType) {
        this.rankType = rankType;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRankPoints() {
        return rankPoints;
    }

    public void setRankPoints(int rankPoints) {
        this.rankPoints = rankPoints;
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

    public List<String> getReward() {
        return reward;
    }

    public void setReward(List<String> reward) {
        this.reward = reward;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getPicRes() {
        return picRes;
    }

    public void setPicRes(String picRes) {
        this.picRes = picRes;
    }

    public int getAwardType() {
        return awardType;
    }

    public void setAwardType(int awardType) {
        this.awardType = awardType;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
