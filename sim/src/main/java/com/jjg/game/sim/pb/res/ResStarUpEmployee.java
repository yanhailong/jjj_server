package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_STAR_UP_EMPLOYEE, resp = true)
@ProtoDesc("升星雇员返回")
public class ResStarUpEmployee extends AbstractResponse {
    @ProtoDesc("雇员id")
    public int employeeId;
    @ProtoDesc("新星级")
    public int star;

    public ResStarUpEmployee(int code) {
        super(code);
    }
}
