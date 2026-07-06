package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE, cmd = SuperGolfConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqSuperGolfStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
