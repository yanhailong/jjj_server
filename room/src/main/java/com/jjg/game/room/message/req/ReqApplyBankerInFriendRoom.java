package com.jjg.game.room.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 请求申请上庄
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.ReqMsgBean.REQ_APPLY_BANKER
)
@ProtoDesc("请求申请上庄")
public class ReqApplyBankerInFriendRoom {

    @ProtoDesc("准备金数量")
    public long predictCostGold;

    @ProtoDesc("操作")
    public int operate;
}
