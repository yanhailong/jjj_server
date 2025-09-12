package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;

/**
 * @author 11
 * @date 2025/9/12 11:02
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CLEOPATRA, cmd = CleopatraConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求获取奖池")
public class ReqCleopatraPool extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
