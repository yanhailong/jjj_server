package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求重置玩家邀请码
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.FRIEND_ROOM_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_RESET_INVITATION_CODE
)
@ProtoDesc("请求重置玩家邀请码")
public class ReqResetInvitationCode extends AbstractMessage {

}
