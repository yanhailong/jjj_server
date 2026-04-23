package com.jjg.game.activity.grandroulette.data;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/4/22 17:58
 */
@ProtobufMessage
@ProtoDesc("大转盘历史信息")
public class GrandRouletteRecord {
    /**
     * 玩家id
     */
    private long playerId;
    /**
     * 领取的金币数量
     */
    private long num;
    /**
     * 达成时间
     */
    private long reachedTimes;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }

    public long getReachedTimes() {
        return reachedTimes;
    }

    public void setReachedTimes(long reachedTimes) {
        this.reachedTimes = reachedTimes;
    }
}
