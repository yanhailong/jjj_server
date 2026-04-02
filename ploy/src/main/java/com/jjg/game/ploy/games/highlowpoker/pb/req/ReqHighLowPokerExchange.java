package com.jjg.game.ploy.games.highlowpoker.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;

/**
 * @author lm
 * @date 2026/4/1 11:49
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.REQ_HIGH_LOW_POKER_EXCHANGE)
@ProtoDesc("兑换金币")
public class ReqHighLowPokerExchange extends AbstractMessage {
}
