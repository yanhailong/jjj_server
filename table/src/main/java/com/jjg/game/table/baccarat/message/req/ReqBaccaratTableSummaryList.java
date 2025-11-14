package com.jjg.game.table.baccarat.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    cmd = BaccaratMessageConstant.ReqMsgBean.REQ_BACCARAT_TABLE_SUMMARY_LIST
)
@ProtoDesc("请求获取百家乐房间摘要信息")
public class ReqBaccaratTableSummaryList extends AbstractMessage {
}
