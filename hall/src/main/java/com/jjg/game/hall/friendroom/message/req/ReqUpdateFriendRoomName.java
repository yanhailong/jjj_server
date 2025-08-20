package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求更新房间名
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_CHANGE_FRIEND_ROOM_NAME
)
@ProtoDesc("请求更新房间名")
public class ReqUpdateFriendRoomName extends AbstractMessage {

    @ProtoDesc("新的名字")
    public String newName;

    @ProtoDesc("房间ID")
    public long roomId;
}
