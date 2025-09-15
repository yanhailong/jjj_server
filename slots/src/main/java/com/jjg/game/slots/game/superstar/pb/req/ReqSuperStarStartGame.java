package com.jjg.game.slots.game.superstar.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqSuperStarStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
