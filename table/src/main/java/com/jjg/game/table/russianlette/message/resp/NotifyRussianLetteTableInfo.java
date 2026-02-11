package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

import java.util.List;

/**
 * 俄罗斯转盘桌面信息，下注，结算，断线重连
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteMessageConstant.RespMsgBean.NOTIFY_RUSSIAN_LETTE_TABLE_INFO,
    resp = true
)
@ProtoDesc("俄罗斯转盘桌面信息，下注，结算，断线重连")
public class NotifyRussianLetteTableInfo extends AbstractNotice {

    @ProtoDesc("基础的牌桌信息")
    public BaseDiceTableInfo baseDiceTableInfo;

    @ProtoDesc("结算历史，首次进入时发送，下注区域id列表")
    public List<RussianLetteHistoryBean> settlementHistory;

    @ProtoDesc("结算信息，当阶段为结算时发送")
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
