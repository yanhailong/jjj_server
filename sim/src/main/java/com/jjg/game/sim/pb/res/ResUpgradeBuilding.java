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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_UPGRADE_BUILDING, resp = true)
@ProtoDesc("升级建筑返回")
public class ResUpgradeBuilding extends AbstractResponse {
    @ProtoDesc("建筑id")
    public int id;
    @ProtoDesc("CD结束时间(ms)")
    public long cdEndTime;

    public ResUpgradeBuilding(int code) {
        super(code);
    }
}
