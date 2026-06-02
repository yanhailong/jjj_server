package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/6/1
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_BUILDING_INFO, resp = true)
@ProtoDesc("获取建筑信息返回")
public class ResBuildingInfo extends AbstractResponse {
    @ProtoDesc("建筑id")
    public int id;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("主管id ,0表示没有主管")
    public int managerEmployId;

    public ResBuildingInfo(int code) {
        super(code);
    }
}
