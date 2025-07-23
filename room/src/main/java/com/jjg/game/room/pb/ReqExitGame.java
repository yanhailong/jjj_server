package com.jjg.game.room.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * @author 11
 * @date 2025/7/15 15:03
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ROOM_TYPE, cmd = RoomMessageConstant.ReqMsgBean.REQ_EXIT_GAME)
@ProtoDesc("退出游戏请求")
public class ReqExitGame extends AbstractMessage {
}
