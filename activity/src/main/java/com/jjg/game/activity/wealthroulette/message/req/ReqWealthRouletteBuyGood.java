package com.jjg.game.activity.wealthroulette.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/12/1 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_WEALTH_ROULETTE_BUY_GOOD)
@ProtoDesc("财富轮盘购买商品")
public class ReqWealthRouletteBuyGood extends AbstractMessage {
    @ProtoDesc("购买数量")
    public int buyNum;
    @ProtoDesc("商品Id")
    public int goodId;
}
