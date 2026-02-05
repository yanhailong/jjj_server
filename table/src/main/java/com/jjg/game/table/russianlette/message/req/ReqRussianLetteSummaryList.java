package com.jjg.game.table.russianlette.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE,
    cmd = RussianLetteMessageConstant.ReqMsgBean.REQ_RUSSIAN_LETTE_SUMMARY_LIST
)
@ProtoDesc("请求获取俄罗斯转盘房间摘要信息")
public class ReqRussianLetteSummaryList extends AbstractMessage {
}
