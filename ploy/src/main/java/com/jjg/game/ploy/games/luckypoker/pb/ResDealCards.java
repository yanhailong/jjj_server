package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = LuckyPokerConstant.MsgBean.RES_DEAL_CARDS, resp = true)
@ProtoDesc("发牌返回")
public class ResDealCards extends AbstractResponse {
    @ProtoDesc("玩家等级")
    public int level;
    @ProtoDesc("经验")
    public long exp;
    @ProtoDesc("累计中奖金币")
    public long allWinGold;
    @ProtoDesc("玩家当前金币")
    public long allGold;
    @ProtoDesc("牌id")
    public List<Integer> pokerIds;
    @ProtoDesc("牌型  0.散牌  1.对J或者更大  2.两对  3.三张  4.顺子  5.同花  6.葫芦  7.四条  8.同花顺  9.皇家同花顺")
    public int pokerRank;
    @ProtoDesc("中奖的牌id")
    public List<Integer> pokerRankPokerIds;

    public ResDealCards(int code) {
        super(code);
    }
}
