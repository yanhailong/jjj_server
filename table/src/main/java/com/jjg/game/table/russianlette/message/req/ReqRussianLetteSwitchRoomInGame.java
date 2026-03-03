package com.jjg.game.table.russianlette.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;

/**
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = RussianLetteMessageConstant.ReqMsgBean.REQ_SWITCH_ROOM_IN_GAME
)
@ProtoDesc("俄罗斯转盘在游戏中请求切换房间")
public class ReqRussianLetteSwitchRoomInGame extends AbstractMessage {

    @ProtoDesc("房间ID")
    public long roomId;

//    @ProtoDesc("游戏类型")
//    public int gameType;

//    @ProtoDesc("场次id")
//    public int wareId;
}
