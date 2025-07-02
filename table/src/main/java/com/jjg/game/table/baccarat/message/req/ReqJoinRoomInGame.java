package com.jjg.game.table.baccarat.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = BaccaratMessageConstant.ReqMsgBean.REQ_JOIN_ROOM_IN_GAME
)
@ProtoDesc("在游戏中请求加入房间")
public class ReqJoinRoomInGame {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("游戏类型")
    public int gameType;

    @ProtoDesc("场次id")
    public int wareId;
}
