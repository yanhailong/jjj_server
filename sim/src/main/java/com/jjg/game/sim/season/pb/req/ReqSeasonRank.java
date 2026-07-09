package com.jjg.game.sim.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SEASON_RANK)
@ProtoDesc("请求当前赛季排行榜")
public class ReqSeasonRank extends AbstractMessage {
    @ProtoDesc("期望返回数量，服务端会限制上限")
    public int limit;
}
