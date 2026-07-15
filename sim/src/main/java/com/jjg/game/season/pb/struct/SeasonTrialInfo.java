package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("试炼关卡信息")
public class SeasonTrialInfo {
    @ProtoDesc("关卡序号 (1起)")
    public int trialId;
    @ProtoDesc("所属解锁天 (task表day字段, 1=赛季开启当天)")
    public int day;
    @ProtoDesc("是否已解锁 (到达天数且前一天关卡全部通关)")
    public boolean unlocked;
    @ProtoDesc("已达成最高星级 (0=未通关)")
    public int stars;
    @ProtoDesc("1/2/3星对应的任务表id (描述/目标/奖励按任务表配置读取)")
    public List<KVInfo> taskIds;
    @ProtoDesc("是否为进行中的挑战")
    public boolean active;
    @ProtoDesc("进行中挑战已用局数")
    public int spinCount;
    @ProtoDesc("挑战窗口总局数 (被动型充值关卡=0)")
    public int expectedSpins;
    @ProtoDesc("当前进度值 (进行中挑战进度 / 充值关卡当前累计值)")
    public long progress;
}
