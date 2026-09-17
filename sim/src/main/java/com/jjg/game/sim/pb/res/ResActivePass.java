package com.jjg.game.sim.pb.res;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.pb.struct.ActivePassInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_ACTIVE_PASS, resp = true)
@ProtoDesc("活跃通行证操作返回")
public class ResActivePass extends AbstractResponse {
    /** 对应请求的完整cmd。 */
    public int requestCmd;
    public ActivePassInfo info;
    public List<ItemInfo> items;
    public ResActivePass() { super(0); }
    public ResActivePass(int code) { super(code); }
}
