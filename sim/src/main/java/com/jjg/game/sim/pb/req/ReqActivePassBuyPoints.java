package com.jjg.game.sim.pb.req;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_ACTIVE_PASS_BUY_POINTS)
@ProtoDesc("购买活跃通行证积分")
public class ReqActivePassBuyPoints extends AbstractMessage {
    public int passId;
    /** 购买份数；每份道具价格和积分量读取global 351。 */
    public int count;
    /** 客户端上次收到的累计购买积分，用于拒绝重复请求。 */
    public int expectedPurchasedPoints;
}
