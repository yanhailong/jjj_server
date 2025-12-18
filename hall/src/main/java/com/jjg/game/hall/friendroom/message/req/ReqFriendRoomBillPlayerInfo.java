package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求好友房账单，单个账单点击时获取整场所有玩家信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_BILL_PLAYER_INFO
)
@ProtoDesc("请求好友账单数据中单局玩家数据")
public class ReqFriendRoomBillPlayerInfo extends AbstractMessage {

    @ProtoDesc("账单ID")
    public long id;
    @ProtoDesc("玩家id")
    public long playerId;
}
