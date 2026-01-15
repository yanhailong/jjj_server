package com.jjg.game.slots.game.acedj.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.acedj.AceDjConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACE_DJ, cmd = AceDjConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("请求奖池信息")
public class ReqAceDjPoolInfo extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
