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
}
