package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * 百人牌桌请求玩家展示数据
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = TableRoomMessageConstant.ReqMsgBean.REQ_TABLE_PLAYER_INFO
)
@ProtoDesc("请求获取百人牌桌的玩家信息")
public class ReqTablePlayerInfo extends AbstractMessage {
}
