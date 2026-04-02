package com.jjg.game.ploy.games.highlowpoker.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;

/**
 * @author lm
 * @date 2026/4/1 11:49
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.RES_HIGH_LOW_POKER_EXCHANGE, resp = true)
@ProtoDesc("兑换金币")
public class ResHighLowPokerExchange extends AbstractResponse {
    @ProtoDesc("兑换金币数")
    public long getGoldNum;

    public ResHighLowPokerExchange(int code) {
        super(code);
    }
}
