package com.jjg.game.slots.game.hotfootball.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HOT_FOOTBALL_TYPE, cmd = HotFootballConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("request pool value")
public class ReqHotFootballPoolValue extends AbstractMessage {
    @ProtoDesc("stake value")
    public long stakeVlue;
}
