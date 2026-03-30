package com.jjg.game.slots.game.luckymouse.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.LUCKY_MOUSE, cmd = LuckyMouseConstant.MsgBean.REQ_LUCKY_MOUSE_POOL_INFO)
@ProtoDesc("获取奖池信息")
public class ReqLuckyMousePoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
