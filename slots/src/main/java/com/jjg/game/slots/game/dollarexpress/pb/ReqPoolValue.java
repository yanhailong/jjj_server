package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;

/**
 * @author 11
 * @date 2025/8/1 15:46
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = DollarExpressConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求奖池金额")
public class ReqPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
