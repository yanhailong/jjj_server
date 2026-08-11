package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/** 客户端通知服务端，本回合全部结算动画已经播放完成。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE,
        cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_SETTLEMENT_ANIMATION_COMPLETE)
@ProtoDesc("斗仙牌结算动画播放完成请求")
public class ReqDouXianSettlementAnimationComplete extends AbstractMessage {

    @ProtoDesc("已完成结算动画的回合，必须与服务端当前回合一致")
    public int round;
}
