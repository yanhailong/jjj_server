package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/21 18:26
 */
@ProtobufMessage
public class GameWareInfo {
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("房间场次ID")
    public int roomCfgId;
    @ProtoDesc("是否是好友房")
    public boolean isFriendRoom;
}
