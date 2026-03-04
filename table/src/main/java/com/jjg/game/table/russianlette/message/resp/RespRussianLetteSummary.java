package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * 返回单个俄罗斯转盘房间摘要信息
 *
 * @author lhc
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_SUMMARY
)
@ProtoDesc("返回单个俄罗斯转盘房间摘要")
public class RespRussianLetteSummary extends AbstractResponse {

    @ProtoDesc("房间单条摘要信息")
    public RussianLetteSingleRes summary;

    public RespRussianLetteSummary(int code) {
        super(code);
    }
}
