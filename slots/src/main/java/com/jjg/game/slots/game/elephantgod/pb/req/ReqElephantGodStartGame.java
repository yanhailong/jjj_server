package com.jjg.game.slots.game.elephantgod.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ELEPHANT_GOD, cmd = ElephantGodConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求配置信息")
public class ReqElephantGodStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
