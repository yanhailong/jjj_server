package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.thor.ThorConstant;

/**
 * @author 11
 * @date 2025/12/1 18:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("进入游戏，返回配置信息")
public class ResThorEnterGame extends AbstractResponse {
    public ResThorEnterGame(int code) {
        super(code);
    }
}
