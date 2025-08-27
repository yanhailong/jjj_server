package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 返回编辑庄家预付金
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.RES_EDIT_BANKER_PREDICATE_GOLD,
    resp = true
)
@ProtoDesc("返回修改庄家预付金")
public class ResEditBankerPredicateGold extends AbstractResponse {

    @ProtoDesc("最新的预付金额")
    public long newlyPredicateGold;

    @ProtoDesc("庄家剩余金额")
    public long bankerResetGold;

    public ResEditBankerPredicateGold(int code) {
        super(code);
    }
}
