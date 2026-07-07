package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 进入协作房间返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.RES_ENTER_COOP_ROOM, resp = true)
@ProtoDesc("进入协作房间返回")
public class ResEnterCoopRoom extends AbstractResponse {
    @ProtoDesc("房间快照")
    public CoopRoomSnapshot room;

    public ResEnterCoopRoom(int code) {
        super(code);
    }
}
