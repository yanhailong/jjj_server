package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/30 14:08
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean
        .NOTIFY_BET, resp = true)
@ProtoDesc("响应下注")
public class NotifyTexasBet extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("下一个执行的玩家id")
    public long nextPlayerId;
    @ProtoDesc("超时时间")
    public long overTime;
    @ProtoDesc("下注金额")
    public long betValue;
    @ProtoDesc("下注类型 5.正常下注 5.ALL_IN")
    public int betType;
}
