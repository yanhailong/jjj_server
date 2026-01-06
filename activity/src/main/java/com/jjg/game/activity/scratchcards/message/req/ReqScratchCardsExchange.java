package com.jjg.game.activity.scratchcards.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/1/5 09:57
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_SCRATCH_CARDS_EXCHANGE)
@ProtoDesc("刮刮乐请求兑换道具")
public class ReqScratchCardsExchange extends AbstractMessage {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("商品id")
    public int goodsId;
}
