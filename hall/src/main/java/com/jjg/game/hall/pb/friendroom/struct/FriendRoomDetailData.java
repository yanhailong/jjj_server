package com.jjg.game.hall.pb.friendroom.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 好友房详细数据
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("好友房详细数据")
public class FriendRoomDetailData {

    @ProtoDesc("在线人数")
    public int onlineNum;

    @ProtoDesc("剩余时间")
    public int remainingTime;
}
