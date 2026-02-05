package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;


/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_SUMMARY
)
@ProtoDesc("返回房间单条摘要信息")
public class NotifyRussianLetteSummary extends AbstractNotice {

    @ProtoDesc("房间单条摘要信息")
    public RussianLetteSingleRes tableSummary;
}
