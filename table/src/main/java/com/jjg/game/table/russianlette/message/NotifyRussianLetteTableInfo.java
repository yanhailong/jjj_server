package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.russianlette.constant.RussianLetteConstant;

import java.util.List;

/**
 * 俄罗斯转盘桌面信息，下注，结算，断线重连
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteConstant.RespMsgBean.NOTIFY_ANIMALS_TABLE_INFO,
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
}
