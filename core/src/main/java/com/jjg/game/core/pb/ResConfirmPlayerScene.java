package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 返回玩家处于哪个场景中
 *
 * @author 2CL
 */
@ProtoDesc("返回确认玩家处于哪个场景中")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = MessageConst.CoreMessage.RES_CONFIRM_PLAYER_SCENE,
    resp = true
)
public class ResConfirmPlayerScene extends AbstractResponse {

    @ProtoDesc("场景类型")
    public ESceneType sceneType;

    public ResConfirmPlayerScene(int code) {
        super(code);
    }
}
