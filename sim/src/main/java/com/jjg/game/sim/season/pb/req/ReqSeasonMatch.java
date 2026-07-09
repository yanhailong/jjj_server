package com.jjg.game.sim.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SEASON_MATCH)
@ProtoDesc("发起赛季异步匹配")
public class ReqSeasonMatch extends AbstractMessage {
    @ProtoDesc("游戏ID")
    public int gameType;
    @ProtoDesc("下注赛季币")
    public long stake;
}
