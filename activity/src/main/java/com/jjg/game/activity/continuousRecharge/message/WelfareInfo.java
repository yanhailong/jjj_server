package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 累计福利活动信息响应
 * @author 11
 * @date 2026/3/5
 */
@ProtobufMessage
@ProtoDesc("累计福利活动信息")
public class WelfareInfo  {
    @ProtoDesc("每日任务列表")
    public List<WelfareTaskInfo> dailyTaskList;
    @ProtoDesc("本月累计任务列表")
    public List<WelfareTaskInfo> monthlyTaskList;
    @ProtoDesc("本月累计充值")
    public String monthlyTotalRecharge;
    @ProtoDesc("今日充值")
    public String todayRecharge;
}
