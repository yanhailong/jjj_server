package com.jjg.game.ploy.games.hillo.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.REQ_HILLO_EXCHANGE)
@ProtoDesc("HILLO exchange")
public class ReqHilloExchange extends AbstractMessage {
}
