package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * @author lm
 * @date 2025/7/23 17:42
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.ReqMsgBean.REQ_ROOM_BASE_INFO
)
public class ReqRoomBaseInfo {
}
