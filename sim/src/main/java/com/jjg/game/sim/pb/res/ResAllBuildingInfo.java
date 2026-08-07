package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.BuildingInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_ALL_BUILDING_INFO, resp = true)
@ProtoDesc("获取所有建筑信息返回")
public class ResAllBuildingInfo extends AbstractResponse {
    @ProtoDesc("当前场景的所有建筑")
    public List<BuildingInfo> buildings;

    public ResAllBuildingInfo(int code) {
        super(code);
    }
}
