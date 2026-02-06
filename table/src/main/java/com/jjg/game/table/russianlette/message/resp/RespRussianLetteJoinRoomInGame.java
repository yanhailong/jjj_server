package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_JOIN_ROOM_IN_GAME
)
@ProtoDesc("俄罗斯转盘在游戏中返回加入房间")
public class RespRussianLetteJoinRoomInGame extends AbstractResponse {

    @ProtoDesc("房间配置ID")
    public int roomCfgId;

    public RespRussianLetteJoinRoomInGame(int code) {
        super(code);
    }
}
