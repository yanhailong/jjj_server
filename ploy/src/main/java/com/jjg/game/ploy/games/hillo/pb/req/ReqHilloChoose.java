package com.jjg.game.ploy.games.hillo.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.REQ_HILLO_CHOOSE)
@ProtoDesc("HILLO 猜牌")
public class ReqHilloChoose extends AbstractMessage {
    @ProtoDesc("投注项 id，-1 表示自动投注推进一步")
    public int chooseId;
}
