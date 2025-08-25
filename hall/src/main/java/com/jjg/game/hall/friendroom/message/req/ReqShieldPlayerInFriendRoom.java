package com.jjg.game.hall.friendroom.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;

/**
 * 请求屏蔽玩家列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = FriendRoomMessageConstant.ReqMsgCons.REQ_SHIELD_PLAYER_LIST
)
@ProtoDesc("请求屏蔽玩家列表")
public class ReqShieldPlayerInFriendRoom extends AbstractMessage {

    @ProtoDesc("玩家ID")
    public long playerId;
}
