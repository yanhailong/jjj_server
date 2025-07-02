package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.RESP_BACCARAT_TABLE_SUMMARY
)
@ProtoDesc("返回房间单条摘要信息")
public class RespBaccaratTableSummary extends AbstractResponse {

    @ProtoDesc("房间单条摘要信息")
    public BaccaratTableSingleRes tableSummary;

    public RespBaccaratTableSummary(int code) {
        super(code);
    }
}
