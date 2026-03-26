package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = LuckyPokerConstant.MsgBean.RES_LUCKY_POKER_BET, resp = true)
@ProtoDesc("下注返回")
public class ResLuckyPokerBet extends AbstractResponse {
    @ProtoDesc("牌id")
    public List<Integer> pokerIds;
    @ProtoDesc("建议保留的牌id")
    public List<Integer> suggestSavePokerIds;

    public ResLuckyPokerBet(int code) {
        super(code);
    }
}
