package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * 通知押注类房间长时间未操作退出提示
 *
 * @author 2CL
 */
@ProtoDesc("通知押注类房间长时间未操作退出的提示")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.RespMsgBean.NOTIFY_TABLE_EXIT_ROOM,
    resp = true
)
public class NotifyTableExitRoom extends AbstractNotice {

    @ProtoDesc("多语言ID")
    public int langId;
}
