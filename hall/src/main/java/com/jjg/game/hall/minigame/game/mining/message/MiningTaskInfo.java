package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;
import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿任务或成就")
public class MiningTaskInfo {
    @ProtoDesc("每日任务类型1格子2深度3工具4矿石5广告，成就为0")
    public int kind;
    @ProtoDesc("每日任务指定道具ID，0不限")
    public int itemId;
    @ProtoDesc("配置ID")
    public int id;
    @ProtoDesc("进度")
    public long progress;
    @ProtoDesc("目标")
    public long target;
    @ProtoDesc("0未完成 1可领取 2已领取")
    public int status;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewards;
}
