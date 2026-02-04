package com.jjg.game.poker.game.tosouth.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;
import com.jjg.game.poker.game.tosouth.message.bean.ToSouthActionInfo;
import com.jjg.game.room.constant.EGamePhase;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.RESP_ROOM_BASE_INFO, resp = true)
@ProtoDesc("响应南方前进房间基本信息")
public class RespToSouthRoomBaseInfo extends AbstractResponse {
    @ProtoDesc("当前阶段")
    public EGamePhase phase;
    @ProtoDesc("玩家基础信息")
    public List<PokerPlayerInfo> playerInfos;
    @ProtoDesc("牌桌操作信息 (仅在出牌阶段有效)")
    public ToSouthActionInfo actionInfo;

    public RespToSouthRoomBaseInfo(int code) {
        super(code);
    }
}
