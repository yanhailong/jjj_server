package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 申请上庄返回
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.RES_APPLY_BANKER,
    resp = true
)
@ProtoDesc("返回申请上庄")
public class ResApplyBankerInFriendRoom extends AbstractResponse {

    public ResApplyBankerInFriendRoom(int code) {
        super(code);
    }
}
