package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("炸弹赔付明细")
public class ToSouthBombDetail {
    @ProtoDesc("赢家 ID")
    public long winnerId;
    @ProtoDesc("输家 ID")
    public long playerId;
    @ProtoDesc("赢金额")
    public long score;
    @ProtoDesc("类型: 1=赢, 2=输")
    public int type;

    public ToSouthBombDetail() {
    }

    public ToSouthBombDetail(long playerId, long score,  int type) {
        this.playerId = playerId;
        this.score = score;
        this.type = type;
    }
}
