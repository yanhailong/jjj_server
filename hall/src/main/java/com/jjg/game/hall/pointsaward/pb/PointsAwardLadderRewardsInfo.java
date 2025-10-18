package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 阶段配置奖励详情
 */
@ProtobufMessage
@ProtoDesc("阶段配置奖励详情")
public class PointsAwardLadderRewardsInfo {

    /**
     * 积分数量
     */
    @ProtoDesc("积分数量")
    private long points;

    /**
     * 奖励的道具id
     */
    @ProtoDesc("奖励的道具id")
    private int itemId;

    /**
     * 奖励的道具数量
     */
    @ProtoDesc("奖励的道具数量")
    private int itemNum;

    /**
     * 是否已经领取 true 已经领取了
     */
    @ProtoDesc("是否已经领取 true 已经领取了")
    private boolean receive;

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemNum() {
        return itemNum;
    }

    public void setItemNum(int itemNum) {
        this.itemNum = itemNum;
    }

    public boolean isReceive() {
        return receive;
    }

    public void setReceive(boolean receive) {
        this.receive = receive;
    }
}
