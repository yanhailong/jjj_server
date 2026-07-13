package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 即时充值复活状态通知，DESIGN.md 8.9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_RECHARGE, resp = true)
@ProtoDesc("斗仙牌通知即时充值复活")
public class NotifyDouXianRecharge extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("状态(1充值中 2充值成功复活 3放弃视为认输)")
    public int state;
    @ProtoDesc("充值倒计时结束时间")
    public long overTime;
}
