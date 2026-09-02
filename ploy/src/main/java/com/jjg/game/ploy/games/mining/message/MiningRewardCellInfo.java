package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿单个奖励来源格子")
public class MiningRewardCellInfo {
    @ProtoDesc("奖励来源格子的绝对行，从1开始")
    public int row;
    @ProtoDesc("奖励来源格子的列，从1开始")
    public int column;
    @ProtoDesc("奖励来源格子的MiningCellType ID")
    public int typeId;
    @ProtoDesc("该格子本次实际产出的奖励")
    public List<ItemInfo> rewards;
}
