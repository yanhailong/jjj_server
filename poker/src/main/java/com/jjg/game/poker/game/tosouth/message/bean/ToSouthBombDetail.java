package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("炸弹赔付明细")
public class ToSouthBombDetail {
    @ProtoDesc("赢家 ID")
    public long winnerId;
    @ProtoDesc("输家 ID")
    public long loserId;
    @ProtoDesc("输赢金额")
    public long score;
    @ProtoDesc("类型: 1=首炸受害者, 2=倒数第二炸弹手")
    public int type;
    
    public ToSouthBombDetail() {}
    
    public ToSouthBombDetail(long winnerId, long loserId, long score, int type) {
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.score = score;
        this.type = type;
    }
}
