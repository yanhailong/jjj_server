package com.jjg.game.slots.game.garaGemstone3.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone3.GaraGemstone3Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_3, cmd = GaraGemstone3Constant.MsgBean.REQ_GARA_GEMSTONE_3_POOL_INFO)
@ProtoDesc("获取奖池信息")
public class ReqGaraGemstone3PoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
