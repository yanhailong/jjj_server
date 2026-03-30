package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;

/**
 * @author 11
 * @date 2025/9/12 11:02
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CLEOPATRA, cmd = CleopatraConstant.MsgBean.RES_POOL_VALUE,resp = true)
@ProtoDesc("奖池返回")
public class ResCleopatraPool extends AbstractResponse {
    @ProtoDesc("奖池")
    public long poolValue;

    public ResCleopatraPool(int code) {
        super(code);
    }
}
