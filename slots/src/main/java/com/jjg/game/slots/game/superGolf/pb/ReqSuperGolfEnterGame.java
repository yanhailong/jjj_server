package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE, cmd = SuperGolfConstant.MsgBean.REQ_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqSuperGolfEnterGame extends AbstractMessage {
}
