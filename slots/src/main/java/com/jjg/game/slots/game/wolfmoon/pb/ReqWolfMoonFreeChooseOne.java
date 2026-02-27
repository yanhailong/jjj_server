package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.REQ_FREE_CHOOSE_ONE)
@ProtoDesc("免费模式三选一")
public class ReqWolfMoonFreeChooseOne extends AbstractMessage {
    @ProtoDesc("0:高赔付 1:固定堆叠百搭 2:递增倍数")
    public int type;
}
