package com.jjg.game.table.common.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.TableRoomMessageConstant;

/**
 * room请求玩家信息
 * @author lm
 * @date 2026/1/6 15:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE, cmd = TableRoomMessageConstant.ReqMsgBean.REQ_PLAYER_INFO)
@ProtoDesc("请求获取玩家信息")
public class ReqPlayerInfo extends AbstractMessage {
}
