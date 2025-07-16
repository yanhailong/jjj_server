package com.jjg.game.room.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;

/**
 * @author 11
 * @date 2025/7/15 15:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.ToServer.REQ_EXIT_GAME)
@ProtoDesc("退出游戏请求")
public class ReqExitGame extends AbstractMessage {
}
