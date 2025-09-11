package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;

/**
 * @author 11
 * @date 2025/8/1 17:48
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CLEOPATRA, cmd = CleopatraConstant.MsgBean.RES_CONFIG_INFO, resp = true)
@ProtoDesc("返回配置信息")
public class ResCleopatraEnterGame extends AbstractResponse {

    public ResCleopatraEnterGame(int code) {
        super(code);
    }
}
