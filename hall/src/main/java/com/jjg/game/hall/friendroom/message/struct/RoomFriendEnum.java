package com.jjg.game.hall.friendroom.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 好友房消息枚举
 *
 * @author 2CL
 */
@ProtobufMessage
public class RoomFriendEnum {

    @ProtobufMessage
    @ProtoDesc("房间好友列表操作枚举")
    public enum ERoomFriendListOperate {
        @ProtoDesc("置顶")
        TOP_UP,
        @ProtoDesc("移除")
        REMOVE,
        @ProtoDesc("取消置顶")
        CANCEL_TOP_UP,
    }
}
