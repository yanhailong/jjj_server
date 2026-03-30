package com.jjg.game.slots.game.tigerbringsriches.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;


@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TigerBringsRichesConstant.MsgBean.REQ_TIGER_BRINGS_RICHES_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqTigerBringsRichesPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
