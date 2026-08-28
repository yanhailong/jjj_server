package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.OperationBuildingCapacity;

import java.util.List;

/**
 * 数据看板实时容纳人数。
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.RES_OPERATION_CAPACITY, resp = true)
@ProtoDesc("数据看板实时容纳人数返回")
public class ResOperationCapacity extends AbstractResponse {
    @ProtoDesc("当前场景ID")
    public int casinoId;
    @ProtoDesc("当前交互人数")
    public int currentCapacity;
    @ProtoDesc("当前已解锁建筑的总容纳上限")
    public int totalCapacity;
    @ProtoDesc("各游戏区和休息区建筑的实时容纳数据")
    public List<OperationBuildingCapacity> buildings;

    public ResOperationCapacity(int code) {
        super(code);
    }
}
