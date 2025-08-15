package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求通过邀请码关注
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_JOIN_ROOM_BY_INVITATION_CODE
)
@ProtoDesc("请求通过邀请码关注玩家")
public class ReqFollowByInvitationCode  extends AbstractMessage {

    @ProtoDesc("邀请码")
    public int invitationCode;
}
