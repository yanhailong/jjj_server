package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = LuckyPokerConstant.MsgBean.REQ_DEAL_CARDS)
@ProtoDesc("请求发牌")
public class ReqDealCards extends AbsNodeMessage {
    @ProtoDesc("保留的牌id")
    public List<Integer> pokerIds;
}
