package com.jjg.game.room.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * @author 11
 * @date 2025/7/15 15:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ROOM_TYPE, cmd = RoomMessageConstant.RespMsgBean.RES_EXIT_GAME,resp = true)
@ProtoDesc("退出游戏")
public class ResExitGame extends AbstractResponse {
    public ResExitGame(int code) {
        super(code);
    }
}
