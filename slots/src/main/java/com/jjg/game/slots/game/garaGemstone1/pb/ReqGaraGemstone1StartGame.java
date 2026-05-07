package com.jjg.game.slots.game.garaGemstone1.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone1.GaraGemstone1Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_1, cmd = GaraGemstone1Constant.MsgBean.REQ_GARA_GEMSTONE_1_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqGaraGemstone1StartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
