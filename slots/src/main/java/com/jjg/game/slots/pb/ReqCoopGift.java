package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 请求赠送房间互动道具。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.REQ_COOP_GIFT)
@ProtoDesc("请求赠送房间互动道具")
public class ReqCoopGift extends AbstractMessage {
    @ProtoDesc("受赠玩家id")
    public long targetId;
    @ProtoDesc("互动道具id")
    public int giftId;
}
