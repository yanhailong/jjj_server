package com.jjg.game.hall.minigame.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.constant.MinigameConstant;

/**
 * 请求开启的小游戏列表
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = MinigameConstant.Message.REQ_MINIGAME_LIST
)
@ProtoDesc("请求开启的小游戏列表")
public class ReqMinigameList extends AbstractMessage {
}
