package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求加入好友房
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_JOIN_FRIEND_ROOM
)
@ProtoDesc("请求加入好友房,进入自己的好友房也走这个协议")
public class ReqJoinFriendRoom extends AbstractMessage {

    @ProtoDesc("好友ID，如果是玩家自己，发玩家自己的playerId")
    public long playerId;

    @ProtoDesc("房间ID")
    public long roomId;
}
