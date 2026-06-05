package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.BuildingInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/4
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SWITCH_CASINO, resp = true)
@ProtoDesc("切换场景返回")
public class ResSwitchCasino extends AbstractResponse {
    @ProtoDesc("当前所在场景id")
    public int currentCasinoId;
    @ProtoDesc("建筑信息")
    public List<BuildingInfo> buildings;
    @ProtoDesc("主管信息 建筑类型->雇员id")
    public List<KVInfo> managerEmployInfos;
    @ProtoDesc("知名度")
    public long awareness;

    public ResSwitchCasino(int code) {
        super(code);
    }
}
