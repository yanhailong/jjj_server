package com.jjg.game.hall.pb.friendroom.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 单个好友房数据
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("好友房数据")
public class FriendRoomBaseData {

    @ProtoDesc("房间ID")
    public int roomId;

    @ProtoDesc("房间自定义名")
    public int roomAliasName;

    @ProtoDesc("游戏ID")
    public int gameId;

    @ProtoDesc("1. 运行中 2. 暂停中 3. 解散中")
    public int roomStatus;
}
