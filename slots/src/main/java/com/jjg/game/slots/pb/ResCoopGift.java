package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 赠送房间互动道具返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON, cmd = SlotsConst.SlotsCommon.RES_COOP_GIFT, resp = true)
@ProtoDesc("赠送房间互动道具返回")
public class ResCoopGift extends AbstractResponse {
    @ProtoDesc("赠送后剩余钻石")
    public long diamond;

    public ResCoopGift(int code) {
        super(code);
    }
}
