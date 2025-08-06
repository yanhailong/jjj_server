package com.jjg.game.table.baccarat.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * 进入百家乐请求初始信息
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BACCARAT_TYPE,
    cmd = BaccaratMessageConstant.ReqMsgBean.REQ_BACCARAT_TABLE_INFO
)
@ProtoDesc("请求百家乐桌面信息")
public class ReqBaccaratTableInfo extends AbstractMessage {
}
