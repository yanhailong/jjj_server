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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CLEAR_BUILDING_CD, resp = true)
@ProtoDesc("清除建筑升级CD返回")
public class ResClearBuildingCD extends AbstractResponse {
    @ProtoDesc("建筑id")
    public int id;

    public ResClearBuildingCD(int code) {
        super(code);
    }
}
