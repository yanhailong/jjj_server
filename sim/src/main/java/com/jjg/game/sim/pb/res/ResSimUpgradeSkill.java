package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_UPGRADE_SKILL, resp = true)
@ProtoDesc("获取slots技能返回")
public class ResSimUpgradeSkill extends AbstractResponse {

    public ResSimUpgradeSkill(int code) {
        super(code);
    }
}
