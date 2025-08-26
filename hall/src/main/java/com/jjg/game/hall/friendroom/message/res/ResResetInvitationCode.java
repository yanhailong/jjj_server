package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 返回邀请码重置
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_RESET_INVITATION_CODE,
    resp = true
)
@ProtoDesc("刷新重置邀请码")
public class ResResetInvitationCode extends AbstractResponse {

    @ProtoDesc("新的邀请码")
    public int invitationCode;

    @ProtoDesc("剩余次数")
    public int resetTimes;

    public ResResetInvitationCode(int code) {
        super(code);
    }
}
