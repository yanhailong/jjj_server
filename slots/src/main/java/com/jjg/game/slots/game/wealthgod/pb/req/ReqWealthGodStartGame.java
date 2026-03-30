package com.jjg.game.slots.game.wealthgod.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqWealthGodStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
