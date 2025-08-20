package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求领取好友房中账单奖励
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_TAKE_FRIEND_ROOM_BILL_INCOME
)
@ProtoDesc("请求领取好友房中收益奖励")
public class ReqTakeFriendRoomIncomeReward extends AbstractMessage {

}
