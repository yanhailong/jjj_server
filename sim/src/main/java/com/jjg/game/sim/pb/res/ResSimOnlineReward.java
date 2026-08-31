package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.OnlineRewardInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_ONLINE_REWARD, resp = true)
@ProtoDesc("获取在线收益信息返回")
public class ResSimOnlineReward extends AbstractResponse {
    @ProtoDesc("在线收益信息")
    public OnlineRewardInfo info;

    public ResSimOnlineReward(int code) {
        super(code);
    }
}
