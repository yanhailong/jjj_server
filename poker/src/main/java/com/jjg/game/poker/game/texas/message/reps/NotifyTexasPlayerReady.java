package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/9/19 15:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE,cmd = TexasConstant.MsgBean.NOTIFY_TEXAS_PLAYER_READY,resp = true)
@ProtoDesc("通知德州玩家进行准备")
public class NotifyTexasPlayerReady extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
}
