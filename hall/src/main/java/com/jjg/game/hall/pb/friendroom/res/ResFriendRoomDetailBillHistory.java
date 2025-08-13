package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 返回好友房详细账单历史
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_FRIEND_ROOM_DETAIL_BILL_HISTORY,
    resp = true
)
@ProtoDesc("返回好友房详细账单历史")
public class ResFriendRoomDetailBillHistory extends AbstractResponse {

    public ResFriendRoomDetailBillHistory(int code) {
        super(code);
    }
}
