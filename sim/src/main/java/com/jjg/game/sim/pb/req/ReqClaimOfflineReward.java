package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/29
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_CLAIM_OFFLINE_REWARD)
@ProtoDesc("领取离线收益")
public class ReqClaimOfflineReward extends AbstractMessage {
    @ProtoDesc("是否看广告领取 (true 广告倍数, false 1倍)")
    public boolean watchAd;
}
