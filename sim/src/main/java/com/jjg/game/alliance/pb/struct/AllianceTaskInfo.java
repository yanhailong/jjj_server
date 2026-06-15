package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟任务项 (池中任务与已接取任务共用, 已接取时带进度)。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟任务信息")
public class AllianceTaskInfo {
    @ProtoDesc("任务实例uid")
    public long uid;
    @ProtoDesc("任务配置id")
    public int cfgId;
    @ProtoDesc("品质 1低 2中 3高")
    public int quality;
    @ProtoDesc("目标类型(AllianceConst.TaskGoalType)")
    public int goalType;
    @ProtoDesc("目标参数")
    public long goalParam;
    @ProtoDesc("目标数量")
    public long goalCount;
    @ProtoDesc("当前进度(池中任务为0)")
    public long progress;
    @ProtoDesc("截止时间(ms): 池中为可接取截止, 已接取为完成截止")
    public long expireTime;
    @ProtoDesc("奖励贡献值")
    public long rewardContribution;
    @ProtoDesc("奖励联盟声誉值")
    public long rewardReputation;
}
