package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.REQ_CHOOSE_WARE)
@ProtoDesc("选择游戏场次进入")
public class ReqChooseWare extends AbstractMessage {
    @ProtoDesc("场次id")
    public int wareId;
}
