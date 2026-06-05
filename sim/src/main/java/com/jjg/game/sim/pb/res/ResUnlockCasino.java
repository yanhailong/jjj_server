package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/6/4
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_UNLOCK_CASINO, resp = true)
@ProtoDesc("开辟新场景返回")
public class ResUnlockCasino extends AbstractResponse {
    @ProtoDesc("场景id")
    public int casinoId;

    public ResUnlockCasino(int code) {
        super(code);
    }
}
