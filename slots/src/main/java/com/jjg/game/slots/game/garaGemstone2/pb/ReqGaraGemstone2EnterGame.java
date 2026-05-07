package com.jjg.game.slots.game.garaGemstone2.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone2.GaraGemstone2Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_2, cmd = GaraGemstone2Constant.MsgBean.REQ_GARA_GEMSTONE_2_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqGaraGemstone2EnterGame extends AbstractMessage {
}
