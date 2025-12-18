package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = SlotsConst.SlotsCommon.RES_SLOTS_ROOM_POOL,resp = true)
@ProtoDesc("返回slots好友房获取准备金")
public class ResSlotsRoomPool extends AbstractResponse {
    public long value;

    public ResSlotsRoomPool(int code) {
        super(code);
    }
}
