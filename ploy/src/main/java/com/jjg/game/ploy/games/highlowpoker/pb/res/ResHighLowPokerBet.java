package com.jjg.game.ploy.games.highlowpoker.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 09:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.RES_HIGH_LOW_POKER_BET, resp = true)
@ProtoDesc("下注返回")
public class ResHighLowPokerBet extends AbstractResponse {
    @ProtoDesc("剩余牌数")
    public int remainCardNum;
    @ProtoDesc("下注区域赔率 从红色系开始然后黑色系最后大于小于")
    public List<String> chooseRate;
    @ProtoDesc("当前牌")
    public int currentCard;

    public ResHighLowPokerBet(int code) {
        super(code);
    }
}
