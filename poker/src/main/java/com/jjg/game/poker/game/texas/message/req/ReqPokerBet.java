package com.jjg.game.poker.game.texas.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/7/30 14:06
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean
        .REQ_BET)
@ProtoDesc("请求下注")
public class ReqPokerBet extends AbstractMessage {
    @ProtoDesc("类型 4正常下注 5ALL_IN")
    public int betType;
    @ProtoDesc("下注金额")
    public long betValue;
}
