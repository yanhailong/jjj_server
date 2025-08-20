package com.jjg.game.hall.friendroom.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 好友房历史账单
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("好友房账单历史")
public class FriendRoomBillHistory {

    @ProtoDesc("账单ID")
    public long id;

    @ProtoDesc("创建时间")
    public long createdTime;

    @ProtoDesc("总赢分")
    public long totalWin;

    @ProtoDesc("总收益")
    public long totalIncome;

    @ProtoDesc("玩家参数人数")
    public int partInNum;
}
