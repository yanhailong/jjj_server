package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CasinoUpgradeCondition;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CASINO_UPGRADE_CONDITION, resp = true)
@ProtoDesc("获取场景升级条件返回")
public class ResCasinoUpgradeCondition extends AbstractResponse {

    @ProtoDesc("场景升级条件")
    public List<CasinoUpgradeCondition> conditions;

    public ResCasinoUpgradeCondition(int code) {
        super(code);
    }
}
