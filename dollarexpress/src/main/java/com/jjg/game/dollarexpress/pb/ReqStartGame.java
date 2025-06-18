package com.jjg.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;

/**
 * @author 11
 * @date 2025/6/12 17:11
 */
@ProtobufMessage(messageType = DollarExpressConst.MSGBEAN.TYPE, cmd = DollarExpressConst.MSGBEAN.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
