package com.jjg.game.room.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 请求庄家申请列表
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.ReqMsgBean.REQ_BANKER_APPLY_LIST
)
@ProtoDesc("请求庄家申请列表")
public class ReqBankerApplyListInFriendRoom {
}
