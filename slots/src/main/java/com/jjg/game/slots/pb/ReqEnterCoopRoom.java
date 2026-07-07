package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 请求进入协作房间 (切节点后附着; 断线重连复用)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.REQ_ENTER_COOP_ROOM)
@ProtoDesc("请求进入协作房间")
public class ReqEnterCoopRoom extends AbstractMessage {
    @ProtoDesc("房间id")
    public long roomId;
}
