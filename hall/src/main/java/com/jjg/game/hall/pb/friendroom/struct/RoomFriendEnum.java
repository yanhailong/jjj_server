package com.jjg.game.hall.pb.friendroom.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 好友房消息枚举
 *
 * @author 2CL
 */
public class RoomFriendEnum {

    @ProtobufMessage
    @ProtoDesc("房间好友列表操作枚举")
    public enum ERoomFriendListOperate {
        @ProtoDesc("置顶")
        TOP_UP,
        @ProtoDesc("移除")
        REMOVE,
    }

    @ProtoDesc("好友房操作枚举")
    @ProtobufMessage
    public enum EFriendRoomOperate {
        @ProtoDesc("暂停")
        PAUSE,
        @ProtoDesc("重启")
        RESTART,
        @ProtoDesc("解散")
        DISBAND,
    }

}
