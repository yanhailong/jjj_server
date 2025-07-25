package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/7/15 15:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ROOM_TYPE, cmd = MessageConst.RoomMessage.RES_EXIT_GAME,resp = true)
@ProtoDesc("退出游戏")
public class ResExitGame extends AbstractResponse {
    public ResExitGame(int code) {
        super(code);
    }
}
