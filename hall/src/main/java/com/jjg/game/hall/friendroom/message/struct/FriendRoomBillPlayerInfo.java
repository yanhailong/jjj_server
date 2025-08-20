package com.jjg.game.hall.friendroom.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("房间玩家账单信息")
public class FriendRoomBillPlayerInfo {

    @ProtoDesc("基础玩家信息")
    public BaseFriendRoomPlayerInfo baseFriendRoomPlayerInfo;

    @ProtoDesc("账单流水")
    public long billFlow;
}
