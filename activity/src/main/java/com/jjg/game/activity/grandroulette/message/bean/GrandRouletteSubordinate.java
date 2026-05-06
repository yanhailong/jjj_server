package com.jjg.game.activity.grandroulette.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/4/23 10:29
 */
@ProtobufMessage
@ProtoDesc("大转盘个人绑定信息")
public class GrandRouletteSubordinate {
    private long playerId;
    private int bindTime;

    public GrandRouletteSubordinate() {
    }

    public GrandRouletteSubordinate(long playerId, int bindTime) {
        this.playerId = playerId;
        this.bindTime = bindTime;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getBindTime() {
        return bindTime;
    }

    public void setBindTime(int bindTime) {
        this.bindTime = bindTime;
    }
}
