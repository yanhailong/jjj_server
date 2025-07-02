package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

import java.util.List;

/**
 * 百家乐通知新的一局开始
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.NOTIFY_BACCARAT_TABLE_ROUND_START
)
@ProtoDesc("百家乐通知新的一局开始")
public class NotifyBaccaratRoundStart extends AbstractResponse {

    @ProtoDesc("桌面的数据")
    public BaccaratTableInfo baccaratTableInfo;

    public NotifyBaccaratRoundStart(int code) {
        super(code);
    }
}
