package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/7/10
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COMBAT_POWER,resp = true)
@ProtoDesc("获取战力返回")
public class ResCombatPower extends AbstractResponse {
    @ProtoDesc("战力数据")
    public List<KVInfo> combatPowers;

    public ResCombatPower(int code) {
        super(code);
    }
}
