package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

import java.util.List;

/**
 * 请求把手牌摆入某个区域，DESIGN.md 8.6 游戏出牌
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_PLACE_CARD)
@ProtoDesc("斗仙牌请求摆牌")
public class ReqDouXianPlaceCard extends AbstractMessage {
    @ProtoDesc("区域(1凡界 2灵界 3仙界)")
    public int zoneId;
    @ProtoDesc("摆入该区域的手牌(客户端牌id)")
    public List<Integer> cardIds;
}
