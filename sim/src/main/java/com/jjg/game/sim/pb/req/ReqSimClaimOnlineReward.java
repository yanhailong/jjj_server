package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SIM_CLAIM_ONLINE_REWARD)
@ProtoDesc("领取在线收益")
public class ReqSimClaimOnlineReward extends AbstractMessage {
    @ProtoDesc("领取方式: 0=观看视频完成后领取, 1=钻石领取")
    public int type;
}
