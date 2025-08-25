package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.RoomFriendEnum;

/**
 * 请求操作好友房间
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_FRIEND_ROOM
)
@ProtoDesc("请求操作好友房间")
public class ReqOperateFriendRoom  extends AbstractMessage {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("操作类型 1. 暂停 2. 重新开启 3. 解散")
    public int operateCode;
}
