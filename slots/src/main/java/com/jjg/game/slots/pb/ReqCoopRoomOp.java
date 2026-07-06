package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 协作房间操作 (准备/取消准备/开始/退出|解散/踢人)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.REQ_COOP_ROOM_OP)
@ProtoDesc("协作房间操作")
public class ReqCoopRoomOp extends AbstractMessage {
    @ProtoDesc("操作码 1准备 2取消准备 3开始 4退出(房主=解散) 5踢人")
    public int op;
    @ProtoDesc("目标玩家id (踢人用)")
    public long targetId;
}
