package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("人气赛季榜信息")
public class VisitRankInfo {
    public int rank;
    public long playerId;
    public String playerName;
    public int headImgId;
    public int headFrameId;
    public int casinoLevel;
    public long popularity;
    public List<ItemInfo> rewards;
}
