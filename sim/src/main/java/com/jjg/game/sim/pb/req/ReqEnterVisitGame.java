package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_ENTER_VISIT_GAME)
@ProtoDesc("进入客座赌局试玩(切换到房主已解锁游戏的slot节点)")
public class ReqEnterVisitGame extends AbstractMessage {
    @ProtoDesc("房主玩家id")
    public long playerId;
    @ProtoDesc("房主赌场id")
    public int casinoId;
    @ProtoDesc("试玩的游戏类型")
    public int gameType;
}
