package com.jjg.game.hall.friendroom.message.struct;

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
    public long roomId;

    @ProtoDesc("房间自定义名")
    public String roomAliasName;

    @ProtoDesc("游戏ID")
    public int gameId;

    @ProtoDesc("1. 运行中 2. 暂停中 3. 解散中")
    public int roomStatus;

    @ProtoDesc("房间到期时间戳")
    public long overdueTime;

    @ProtoDesc("准备金剩余数量")
    public long predictCostGoldNum;

    @ProtoDesc("在线人数")
    public long onlinePlayerNum;

    @ProtoDesc("最大人数")
    public int maxPlayerNum;

    @ProtoDesc("暂停按钮下一次可用的时间，时间戳，为0或者时间戳到期表示可用")
    public long nextPauseBtnOverdueTime;
}
