package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.REQ_TOGETHER_PLAY_INVITE)
@ProtoDesc("发送好友同玩邀请")
public class ReqTogetherPlayInvite extends AbstractMessage {
    @ProtoDesc("被邀请好友id")
    public long targetPlayerId;
}
