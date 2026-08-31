package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/** 单个成就徽章的任务进度与档位。 */
@ProtobufMessage
@ProtoDesc("成就徽章进度")
public class AchievementBadgeInfo {
    @ProtoDesc("徽章ID，对应 task.BadgeID / MedalBuff.MedalType")
    public int badgeId;
    @ProtoDesc("所属建筑ID，<=0表示全局徽章")
    public int buildingId;
    @ProtoDesc("所属游戏ID(BuildingAreaTable.UnlockGameId)，0表示全局徽章")
    public int gameId;
    @ProtoDesc("已完成任务数")
    public int completedTaskCount;
    @ProtoDesc("任务总数")
    public int totalTaskCount;
    @ProtoDesc("当前激活加成档位配置ID(MedalBuff.id)，0表示尚未达到任何CollectNum档位")
    public int currentBuffCfgId;
    @ProtoDesc("下一档配置ID(MedalBuff.id)，0表示已满档")
    public int nextBuffCfgId;
}
