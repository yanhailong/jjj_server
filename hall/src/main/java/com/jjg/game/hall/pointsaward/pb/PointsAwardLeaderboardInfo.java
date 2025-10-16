package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 积分大奖排行信息
 */
@ProtobufMessage
@ProtoDesc("积分大奖排行信息")
public class PointsAwardLeaderboardInfo {

    /**
     * 名次对应的配置id
     */
    @ProtoDesc("名次对应的配置id")
    private int configId;

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

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }
}
