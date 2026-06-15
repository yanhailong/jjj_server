package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_PURCHASED_GUEST_REWARD)
@ProtoDesc("请求领取购买游客奖励 (按目的地逐个领取)")
public class ReqPurchasedGuestReward extends AbstractMessage {
    @ProtoDesc("购买游客唯一id")
    public long uid;
    @ProtoDesc("目的地序号 (GuestInfo.destinations 下标)")
    public int index;
}
