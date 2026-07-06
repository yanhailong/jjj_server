package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 协作房间操作返回 (状态变更经 NotifyCoopRoomUpdate 广播)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.RES_COOP_ROOM_OP, resp = true)
@ProtoDesc("协作房间操作返回")
public class ResCoopRoomOp extends AbstractResponse {
    @ProtoDesc("操作码")
    public int op;

    public ResCoopRoomOp(int code) {
        super(code);
    }
}
