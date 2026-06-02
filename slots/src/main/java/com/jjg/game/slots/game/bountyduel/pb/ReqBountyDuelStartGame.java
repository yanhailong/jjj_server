package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE, cmd = BountyDuelConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求赏金大对决开始游戏")
public class ReqBountyDuelStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
