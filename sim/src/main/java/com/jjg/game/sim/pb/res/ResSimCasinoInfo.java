package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CASINO_INFO, resp = true)
@ProtoDesc("获取场景信息")
public class ResSimCasinoInfo extends AbstractResponse {
    @ProtoDesc("场景等级")
    public int level;
    @ProtoDesc("场景经验")
    public int exp;
    @ProtoDesc("升级消耗的经验")
    public int upgradeCost;


    public ResSimCasinoInfo(int code) {
        super(code);
    }
}
