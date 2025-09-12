package com.jjg.game.slots.game.wealthgod.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求奖池金额")
public class ReqWealthGodPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
