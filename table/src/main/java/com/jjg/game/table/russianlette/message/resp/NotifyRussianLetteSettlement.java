package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;


/**
 * 通知俄罗斯转盘结算
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知俄罗斯转盘结算信息")
public class NotifyRussianLetteSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public RussianLetteSettlementInfo settlementInfo;

    @ProtoDesc("红色概率")
    public double red;

    @ProtoDesc("黑色概率")
    public double black;

    @ProtoDesc("奇数概率")
    public double odd;

    @ProtoDesc("偶数概率")
    public double event;

    @ProtoDesc("玩家牌桌玩的次数")
    public double playNum;

    @ProtoDesc("累计这次下注总金额（断线重连同步）")
    public double allBet;
}
