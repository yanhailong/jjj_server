package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_BUY_SPECIAL_GUEST)
@ProtoDesc("购买特殊游客")
public class ReqBuySpecialGuest extends AbstractMessage {
    @ProtoDesc("当前展示的特殊游客生成配置id，支持广告、钻石和现金类型")
    public int id;
    @ProtoDesc("付费类型  0.广告  1.钻石  2.美刀")
    public int costType;
    @ProtoDesc("支付方式 1.google 2.ios，仅现金购买时使用")
    public int payType;
    @ProtoDesc("当前展示商品所属的VisitorTargetList配置id")
    public int poolId;
}
