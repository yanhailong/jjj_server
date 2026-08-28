package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.OperationBuildingData;
import com.jjg.game.sim.pb.struct.OperationDashboardOverview;
import com.jjg.game.sim.pb.struct.OperationResearchBuilding;

import java.util.List;

/**
 * 细分运营数据看板完整数据。
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.RES_OPERATION_DASHBOARD, resp = true)
@ProtoDesc("细分运营数据看板完整数据返回")
public class ResOperationDashboard extends AbstractResponse {
    @ProtoDesc("当前场景ID")
    public int casinoId;
    @ProtoDesc("顶部经营数据总览")
    public OperationDashboardOverview overview;
    @ProtoDesc("当前场景已解锁建筑的细分数据")
    public List<OperationBuildingData> buildings;
    @ProtoDesc("当前场景Slot游戏研发列表，按配置解锁顺序排列")
    public List<OperationResearchBuilding> researchBuildings;

    public ResOperationDashboard(int code) {
        super(code);
    }
}
