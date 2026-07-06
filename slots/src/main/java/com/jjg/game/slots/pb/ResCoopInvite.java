package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 发送协作房间邀请返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.COOP_ROOM, cmd = SlotsConst.SlotsCommon.RES_COOP_INVITE, resp = true)
@ProtoDesc("发送协作房间邀请返回")
public class ResCoopInvite extends AbstractResponse {

    public ResCoopInvite(int code) {
        super(code);
    }
}
