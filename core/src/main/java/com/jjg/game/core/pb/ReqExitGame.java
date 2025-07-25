package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/7/15 15:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ROOM_TYPE, cmd = MessageConst.RoomMessage.REQ_EXIT_GAME)
@ProtoDesc("退出游戏请求")
public class ReqExitGame extends AbstractMessage {
}
