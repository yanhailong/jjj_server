package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 请求确认玩家处于哪个场景中
 *
 * @author 2CL
 */
@ProtoDesc("请求确认玩家处于哪个场景中")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = MessageConst.CoreMessage.REQ_CONFIRM_PLAYER_SCENE,
    resp = true
)
public class ReqConfirmPlayerScene extends AbstractMessage {
}
