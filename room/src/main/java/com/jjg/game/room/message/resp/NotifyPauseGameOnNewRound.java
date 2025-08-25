package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 游戏暂停通知
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.NOTIFY_GAME_PAUSE_ON_NEW_ROUND,
    resp = true
)
@ProtoDesc("游戏暂停通知,一般在新的一轮开始时通知")
public class NotifyPauseGameOnNewRound extends AbstractNotice {
}
