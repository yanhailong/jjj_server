package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_RUSSIAN_LETTE_OTHER_SUMMARY_LIST
)
@ProtoDesc("返回俄罗斯转盘其他房间摘要信息")
public class RespRussianLetteOtherSummaryList extends AbstractResponse {

    @ProtoDesc("房间摘要列表")
    public List<RussianLetteSummary> tableSummaryList;

    public RespRussianLetteOtherSummaryList(int code) {
        super(code);
    }
}

