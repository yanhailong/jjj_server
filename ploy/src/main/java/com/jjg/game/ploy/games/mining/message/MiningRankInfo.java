package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;
import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿排行条目")
public class MiningRankInfo {
    @ProtoDesc("排名，0未上榜")
    public int rank;
    @ProtoDesc("玩家ID")
    public long playerId;
    @ProtoDesc("昵称")
    public String nickName;
    @ProtoDesc("头像")
    public int headImgId;
    @ProtoDesc("深度")
    public int depth;
    @ProtoDesc("该名次的奖励")
    public List<ItemInfo> rewards;
}
