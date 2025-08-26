package com.jjg.game.table.baccarat.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * 请求在游戏中退出房间
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = BaccaratMessageConstant.ReqMsgBean.REQ_EXIT_ROOM_IN_GAME
)
@ProtoDesc("在游戏中请求退出房间")
public class ReqExitRoomInGame extends AbstractMessage {
}
