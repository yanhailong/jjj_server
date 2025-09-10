package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;

/**
 * 响应创建好友房消息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_CREAT_FRIENDS_ROOM,
    resp = true
)
@ProtoDesc("响应创建好友房消息")
public class RespCreateFriendsRoom extends AbstractResponse {

    @ProtoDesc("房间基础信息")
    public FriendRoomBaseData roomBaseData;

    @ProtoDesc("邀请码，玩家第一次创建时发送")
    public int invitationCode;

    public RespCreateFriendsRoom(int code) {
        super(code);
    }
}
