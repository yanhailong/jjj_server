package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

import java.util.List;


/**
 * 通知俄罗斯转盘结算
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteMessageConstant.RespMsgBean.NOTIFY_RUSSIAN_LETTE_SUMMARY,
    resp = true
)
@ProtoDesc("通知俄罗斯转盘房间摘要")
public class NotifyRussianLetteTableSummary extends AbstractNotice {
    @ProtoDesc("房间摘要列表")
    public RussianLetteSummary tableSummary;
}
