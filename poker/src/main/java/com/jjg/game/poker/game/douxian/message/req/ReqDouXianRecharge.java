package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 金币耗尽时请求钻石购买金币复活，DESIGN.md 8.9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_RECHARGE)
@ProtoDesc("斗仙牌请求即时充值复活")
public class ReqDouXianRecharge extends AbstractMessage {
    @ProtoDesc("复活礼包配置id")
    public int rechargeOptionId;
}
