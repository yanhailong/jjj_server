package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPairSettlementInfo;

import java.util.List;

/**
 * 单回合结算通知(6组两两结算，凡->灵->仙顺序播放)，DESIGN.md 8.9/四
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_SETTLEMENT, resp = true)
@ProtoDesc("斗仙牌通知结算结果")
public class NotifyDouXianSettlement extends AbstractNotice {
    @ProtoDesc("当前回合")
    public int round;
    @ProtoDesc("两两结算结果列表")
    public List<DouXianPairSettlementInfo> pairResults;
}
