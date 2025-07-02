package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.table.baccarat.message.BaccaratMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = BaccaratMessageConstant.RespMsgBean.RESP_JOIN_ROOM_IN_GAME
)
@ProtoDesc("在游戏中返回加入房间")
public class RespJoinRoomInGame extends AbstractResponse {

    @ProtoDesc("房间配置ID")
    public int roomCfgId;

    public RespJoinRoomInGame(int code) {
        super(code);
    }
}
