package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_EXIT_ROOM_IN_GAME
)
@ProtoDesc("俄罗斯转盘在游戏中返回退出房间")
public class RespRussianLetteExitRoomInGame extends AbstractResponse {

    public RespRussianLetteExitRoomInGame(int code) {
        super(code);
    }
}
