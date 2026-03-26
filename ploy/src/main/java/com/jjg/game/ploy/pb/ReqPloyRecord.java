package com.jjg.game.ploy.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.constant.PloyConstant;

/**
 * @author 11
 * @date 2026/3/20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER, cmd = PloyConstant.MsgBean.REQ_PLOY_RECORD)
@ProtoDesc("请求记录")
public class ReqPloyRecord extends AbsNodeMessage {
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("页码")
    public int pageIndex;
}
