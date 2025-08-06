package com.jjg.game.poker.game.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.poker.game.common.constant.PokerConstant;

/**
 * @author lm
 * @date 2025/7/30 10:15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.REQ_SAMPLE_CARD_OPERATION)
@ProtoDesc("请求过牌,弃牌,停牌")
public class ReqPokerSampleCardOperation extends AbstractMessage {
    @ProtoDesc("类型(1弃牌 2过牌 3停牌)")
    public int type = 0;
}
