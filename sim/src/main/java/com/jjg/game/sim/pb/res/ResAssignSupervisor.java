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
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_ASSIGN_SUPERVISOR, resp = true)
@ProtoDesc("任命主管返回")
public class ResAssignSupervisor extends AbstractResponse {
    @ProtoDesc("雇员id")
    public int employeeId;
    @ProtoDesc("主管百分比加成  key参考BuildingAreaTable表的typeValue值")
    public List<KVInfo> manageEmployeeBonus;
    @ProtoDesc("主管固定加成  key参考BuildingAreaTable表的typeValue值")
    public List<KVInfo> manageEmployeeFixBonus;

    public ResAssignSupervisor(int code) {
        super(code);
    }
}
