package com.jjg.game.slots.game.superstar.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求奖池金额")
public class ReqSuperStarPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
