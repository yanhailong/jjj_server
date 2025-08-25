package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

import java.util.List;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.RESP_BACCARAT_TABLE_SUMMARY_LIST
)
@ProtoDesc("返回房间摘要信息")
public class RespBaccaratTableSummaryList extends AbstractResponse {

    @ProtoDesc("房间摘要列表")
    public List<BaccaratTableSummary> tableSummaryList;

    public RespBaccaratTableSummaryList(int code) {
        super(code);
    }
}
