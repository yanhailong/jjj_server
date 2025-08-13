package com.jjg.game.hall.pb.friendroom.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 玩家流水信息
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("流水信息")
public class FriendRoomPlayerBillInfo extends BaseFriendRoomPlayerInfo {
    public long winSocre;
}
