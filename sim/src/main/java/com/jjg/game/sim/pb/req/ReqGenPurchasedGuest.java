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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_GEN_PURCHASED_GUEST)
@ProtoDesc("请求生成购买游客 (点击购买后触发)")
public class ReqGenPurchasedGuest extends AbstractMessage {
    @ProtoDesc("游客id")
    public int guestId;
    @ProtoDesc("数量")
    public int count;
}
