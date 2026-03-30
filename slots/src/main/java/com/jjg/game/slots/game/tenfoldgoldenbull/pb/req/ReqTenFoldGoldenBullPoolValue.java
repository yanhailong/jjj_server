package com.jjg.game.slots.game.tenfoldgoldenbull.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;


@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = TenFoldGoldenBullConstant.MsgBean.REQ_TEN_FOLD_GOLDEN_BULL_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqTenFoldGoldenBullPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
