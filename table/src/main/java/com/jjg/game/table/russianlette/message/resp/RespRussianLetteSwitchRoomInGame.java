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
    cmd = RussianLetteMessageConstant.RespMsgBean.RESP_SWITCH_ROOM_IN_GAME
)
@ProtoDesc("俄罗斯转盘在游戏中返回切换房间")
public class RespRussianLetteSwitchRoomInGame extends AbstractResponse {

    @ProtoDesc("房间配置ID")
    public int roomCfgId;

    public RespRussianLetteSwitchRoomInGame(int code) {
        super(code);
    }
}
