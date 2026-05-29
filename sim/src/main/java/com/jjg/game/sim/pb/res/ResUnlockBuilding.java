package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_UNLOCK_BUILDING, resp = true)
@ProtoDesc("解锁建筑返回")
public class ResUnlockBuilding extends AbstractResponse {
    @ProtoDesc("建筑id")
    public int id;

    public ResUnlockBuilding(int code) {
        super(code);
    }
}
