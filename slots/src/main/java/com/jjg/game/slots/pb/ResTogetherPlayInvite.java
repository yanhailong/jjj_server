package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.RES_TOGETHER_PLAY_INVITE, resp = true)
@ProtoDesc("发送好友同玩邀请返回")
public class ResTogetherPlayInvite extends AbstractResponse {
    @ProtoDesc("被邀请好友id")
    public long targetPlayerId;

    public ResTogetherPlayInvite(int code) {
        super(code);
    }
}
