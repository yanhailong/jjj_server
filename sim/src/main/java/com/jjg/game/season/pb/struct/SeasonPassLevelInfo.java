package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("通行证等级状态")
public class SeasonPassLevelInfo {
    @ProtoDesc("PassDetails.id")
    public int detailId;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("完成条件多语言id")
    public int conditionId;
    @ProtoDesc("当前进度")
    public long progress;
    @ProtoDesc("目标")
    public long target;
    @ProtoDesc("是否完成")
    public boolean completed;
    @ProtoDesc("免费奖励")
    public SeasonPassRewardsInfo freeRewards;
    @ProtoDesc("初级奖励")
    public SeasonPassRewardsInfo beginnerRewards;
    @ProtoDesc("高级奖励")
    public SeasonPassRewardsInfo advanceRewards;
}
