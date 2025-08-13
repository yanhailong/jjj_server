package com.jjg.game.hall.pb.friendroom.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 2CL
 */
@ProtoDesc("每月好友历史账单")
@ProtobufMessage()
public class FriendRoomBillHistoryMonth {

    @ProtoDesc("月份")
    public int month;

    @ProtoDesc("总收益")
    public int totalIncome;

    @ProtoDesc("账单历史")
    public List<FriendRoomBillHistory> billHistories;
}
