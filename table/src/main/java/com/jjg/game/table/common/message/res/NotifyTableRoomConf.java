package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * 推送押注类房间
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.RespMsgBean.NOTIFY_TABLE_ROOM_CONF,
    resp = true
)
@ProtoDesc("推送押注类房间配置信息,客户端请求房间初始化(ReqRoomBaseInfo)数据之后推送")
public class NotifyTableRoomConf extends AbstractNotice {

    @ProtoDesc("押注桌面展示的筹码数量上限")
    public int maxChipOnTable;
}
