package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBillHistoryMonth;

import java.util.List;

/**
 * 返回好友房详细账单历史
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_FRIEND_ROOM_DETAIL_BILL_HISTORY,
    resp = true
)
@ProtoDesc("返回好友房详细账单历史")
public class ResFriendRoomDetailBillHistory extends AbstractResponse {

    @ProtoDesc("按月分的历史数据")
    public List<FriendRoomBillHistoryMonth> monthBillList;

    @ProtoDesc("分页下标，-1表示到末尾")
    public int pageIdx;

    @ProtoDesc("分页大小")
    public int pageSize;

    public ResFriendRoomDetailBillHistory(int code) {
        super(code);
    }
}
