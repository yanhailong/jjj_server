package com.jjg.game.sim.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_ENTER_GAME, resp = true)
@ProtoDesc("进入模拟经营游戏返回")
public class ResSimEnterGame extends AbstractResponse {
    public ResSimEnterGame(int code) {
        super(code);
    }
}
