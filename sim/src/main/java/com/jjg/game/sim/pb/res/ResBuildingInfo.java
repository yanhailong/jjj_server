package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.BuildingInfo;

/**
 * @author 11
 * @date 2026/6/1
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_BUILDING_INFO, resp = true)
@ProtoDesc("获取建筑信息返回")
public class ResBuildingInfo extends AbstractResponse {
    @ProtoDesc("建筑信息")
    public BuildingInfo buildingInfo;
    @ProtoDesc("配置的观看广告次数")
    public int watchAdLimit;
    @ProtoDesc("主管id")
    public int managerId;
    @ProtoDesc("主管等级")
    public int managerLevel;
    @ProtoDesc("是否有雇员可设置为主管")
    public boolean canSetManager;

    public ResBuildingInfo(int code) {
        super(code);
    }
}
