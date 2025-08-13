package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 请求下庄
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.RES_CANCEL_BE_BANKER,
    resp = true
)
@ProtoDesc("请求下庄")
public class ResCancelBeBankerInFriendRoom extends AbstractResponse {

    public ResCancelBeBankerInFriendRoom(int code) {
        super(code);
    }
}
