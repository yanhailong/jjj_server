package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;

/**
 * 响应通过邀请码关注
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_JOIN_ROOM_BY_INVITATION_CODE,
    resp = true
)
@ProtoDesc("响应通过邀请码关注的玩家")
public class ResFollowByInvitationCode extends AbstractResponse {

    @ProtoDesc("通过邀请码关注的玩家信息，关注后需放在关注列表最前面")
    public BaseFriendRoomPlayerInfo playerInfo;

    public ResFollowByInvitationCode(int code) {
        super(code);
    }
}
