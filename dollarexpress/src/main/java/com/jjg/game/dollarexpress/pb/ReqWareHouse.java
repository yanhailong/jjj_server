package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

/**
 * @author 11
 * @date 2025/6/13 13:36
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.REQ_WARE_HOUSE)
@ProtoDesc("请求场次信息")
public class ReqWareHouse extends AbstractMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
}
