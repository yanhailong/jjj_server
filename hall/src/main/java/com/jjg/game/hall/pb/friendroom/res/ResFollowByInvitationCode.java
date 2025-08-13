package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 响应通过邀请码关注
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_JOIN_ROOM_BY_INVITATION_CODE,
    resp = true
)
@ProtoDesc("响应通过邀请码关注，通过")
public class ResFollowByInvitationCode extends AbstractResponse {


    public ResFollowByInvitationCode(int code) {
        super(code);
    }
}
