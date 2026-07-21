package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

import java.util.List;

/**
 * 弃牌阶段请求，DESIGN.md 8.11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_DISCARD)
@ProtoDesc("斗仙牌请求弃牌")
public class ReqDouXianDiscard extends AbstractMessage {
    @ProtoDesc("是否选择不弃")
    public boolean noDiscard;
    @ProtoDesc("要弃置的手牌(客户端牌id)，noDiscard为true时忽略")
    public List<Integer> cardIds;
}
