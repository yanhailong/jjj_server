package com.jjg.game.hall.pointsaward.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

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

}
