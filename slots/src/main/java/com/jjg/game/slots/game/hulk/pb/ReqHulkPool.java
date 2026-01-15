package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2026/1/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求获取奖池")
public class ReqHulkPool extends AbsNodeMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
