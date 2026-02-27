package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.RES_FREE_CHOOSE_ONE, resp = true)
@ProtoDesc("返回免费模式三选一")
public class ResWolfMoonFreeChooseOne extends AbstractResponse {
    public ResWolfMoonFreeChooseOne(int code) {
        super(code);
    }
}
