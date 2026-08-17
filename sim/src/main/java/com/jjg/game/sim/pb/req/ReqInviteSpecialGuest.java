package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_INVITE_SPECIAL_GUEST)
@ProtoDesc("邀请特殊游客")
public class ReqInviteSpecialGuest extends AbstractMessage {
    @ProtoDesc("当前场景要邀请的特殊游客道具id，传多个可一键邀请")
    public List<Integer> itemIds;
}
