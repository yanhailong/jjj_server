package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 转盘历史记录
 */
@ProtobufMessage
@ProtoDesc("转盘历史记录")
public class PointsAwardTurntableHistory {

    /**
     * 玩家id
     */
    @ProtoDesc("玩家id")
    private long playerId;

    /**
     * 奖励id
     */
    @ProtoDesc("奖励id")
    private int awardId;

    /**
     * 奖励道具
     */
    @ProtoDesc("奖励道具")
    private List<ItemInfo> itemInfoList = new ArrayList<>();

    /**
     * 奖励积分数量
     */
    @ProtoDesc("奖励积分数量")
    private int integralNum;

    /**
     * 时间
     */
    @ProtoDesc("时间")
    private long time;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getAwardId() {
        return awardId;
    }

    public void setAwardId(int awardId) {
        this.awardId = awardId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<ItemInfo> getItemInfoList() {
        return itemInfoList;
    }

    public void setItemInfoList(List<ItemInfo> itemInfoList) {
        this.itemInfoList = itemInfoList;
    }

    public int getIntegralNum() {
        return integralNum;
    }

    public void setIntegralNum(int integralNum) {
        this.integralNum = integralNum;
    }
}
