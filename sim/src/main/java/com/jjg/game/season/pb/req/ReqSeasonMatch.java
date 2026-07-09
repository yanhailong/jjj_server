package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_MATCH)
@ProtoDesc("发起赛季异步匹配")
public class ReqSeasonMatch extends AbstractMessage {
    @ProtoDesc("游戏ID")
    public int gameType;
    @ProtoDesc("下注赛季币")
    public long stake;
}
