package com.jjg.game.ploy.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.constant.PloyConstant;

/**
 * @author 11
 * @date 2026/3/19
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_COMMON, cmd = PloyConstant.MsgBean.REQ_PLOY_BET)
@ProtoDesc("请求下注")
public class ReqPloyBet extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long value;
}
