package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 房间互动道具动效广播。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.NOTIFY_COOP_GIFT, resp = true)
@ProtoDesc("房间互动道具动效广播")
public class NotifyCoopGift extends AbstractResponse {
    @ProtoDesc("赠送者id")
    public long fromId;
    @ProtoDesc("受赠者id")
    public long toId;
    @ProtoDesc("互动道具id")
    public int giftId;

    public NotifyCoopGift(int code) {
        super(code);
    }
}
