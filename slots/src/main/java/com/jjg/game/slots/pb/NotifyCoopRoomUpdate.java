package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 协作房间快照变更广播 (成员进出/准备/开始/解散等低频事件)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.NOTIFY_COOP_ROOM_UPDATE, resp = true)
@ProtoDesc("协作房间快照变更广播")
public class NotifyCoopRoomUpdate extends AbstractResponse {
    @ProtoDesc("房间快照")
    public CoopRoomSnapshot room;
    @ProtoDesc("被踢/解散时为true, 客户端应退出房间界面")
    public boolean removed;

    public NotifyCoopRoomUpdate(int code) {
        super(code);
    }
}
