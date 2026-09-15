package com.jjg.game.common.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("排行榜奖励区间")
public class RankRewardInfo {
    @ProtoDesc("起始排名，包含")
    public int startRank;
    @ProtoDesc("结束排名，包含")
    public int endRank;
    @ProtoDesc("对应奖励")
    public List<ItemInfo> rewards;
}
