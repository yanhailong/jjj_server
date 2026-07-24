package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("赛季排行条目")
public class SeasonRankInfo {
    @ProtoDesc("名次")
    public int rank;
    @ProtoDesc("玩家ID")
    public long playerId;
    @ProtoDesc("玩家昵称")
    public String playerName;
    @ProtoDesc("当前赛季币")
    public long seasonCoin;
    @ProtoDesc("累计获得赛季币")
    public long totalEarnedCoin;
    @ProtoDesc("阶段：1新手，2进阶，3循环")
    public int phase;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
}
