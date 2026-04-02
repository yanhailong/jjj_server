package com.jjg.game.ploy.games.highlowpoker.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowPokerConstant;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 11:49
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HIGH_LOW_POKER, cmd = HighLowPokerConstant.MsgBean.RES_HIGH_LOW_POKER_CHOOSE,resp = true)
@ProtoDesc("选择")
public class ResHighLowPokerChoose extends AbstractResponse {
    @ProtoDesc("下一张牌id")
    public int nextCardId;
    @ProtoDesc("当前能兑现的金币数量")
    public long currentCoin;
    @ProtoDesc("下注区域赔率 从红色系开始然后黑色系最后大于小于 失败没值")
    public List<String> chooseRate;
    @ProtoDesc("兑换金币数量")
    public long exchangeNum;

    public ResHighLowPokerChoose(int code) {
        super(code);
    }
}
