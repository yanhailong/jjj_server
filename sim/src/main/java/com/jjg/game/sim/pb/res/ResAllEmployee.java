package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.EmployDetailInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_ALL_EMPLOYEE, resp = true)
@ProtoDesc("获取所有雇员返回")
public class ResAllEmployee extends AbstractResponse {
    public List<EmployDetailInfo> employees;

    public ResAllEmployee(int code) {
        super(code);
    }
}
