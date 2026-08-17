package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_INVITE_SPECIAL_GUEST, resp = true)
@ProtoDesc("邀请特殊游客返回")
public class ResInviteSpecialGuest extends AbstractResponse {

    public ResInviteSpecialGuest(int code) {
        super(code);
    }
}
