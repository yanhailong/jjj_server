package com.jjg.game.room.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 请求修改庄家预付金币
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.ReqMsgBean.REQ_EDIT_BANKER_PREDICATE_GOLD
)
@ProtoDesc("请求编辑庄家预付金币，只能当前庄家操作")
public class ReqEditBankerPredicateGold {

    @ProtoDesc("预付金币，增加值")
    public long predicateGold;
}
