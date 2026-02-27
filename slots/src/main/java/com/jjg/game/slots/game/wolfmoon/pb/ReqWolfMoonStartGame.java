package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqWolfMoonStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
