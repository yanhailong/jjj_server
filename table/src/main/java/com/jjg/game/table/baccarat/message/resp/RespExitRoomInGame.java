package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.RESP_EXIT_ROOM_IN_GAME
)
@ProtoDesc("在游戏中返回退出房间")
public class RespExitRoomInGame extends AbstractResponse {

    public RespExitRoomInGame(int code) {
        super(code);
    }
}
