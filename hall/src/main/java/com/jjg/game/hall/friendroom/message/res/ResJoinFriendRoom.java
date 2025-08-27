package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 返回加入好友房
 *
 * @author 2CL
 */

@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_JOIN_FRIEND_ROOM,
    resp = true
)
@ProtoDesc("返回加入好友房")
public class ResJoinFriendRoom extends AbstractResponse {

    public ResJoinFriendRoom(int code) {
        super(code);
    }
}
