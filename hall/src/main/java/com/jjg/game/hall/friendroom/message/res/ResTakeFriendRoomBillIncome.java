package com.jjg.game.hall.friendroom.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 返回领取好友房收益奖励
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ResMsgCons.RES_TAKE_FRIEND_ROOM_BILL_INCOME,
    resp = true
)
@ProtoDesc("返回好友房详细账单历史")
public class ResTakeFriendRoomBillIncome extends AbstractResponse {

    public ResTakeFriendRoomBillIncome(int code) {
        super(code);
    }
}
