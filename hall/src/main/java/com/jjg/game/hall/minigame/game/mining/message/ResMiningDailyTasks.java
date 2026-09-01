package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_DAILY_TASKS, resp = true)
@ProtoDesc("挖矿每日任务")
public class ResMiningDailyTasks extends AbstractResponse {
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("每日刷新时间毫秒")
    public long nextDailyReset;
    @ProtoDesc("每日任务")
    public List<MiningTaskInfo> dailyTasks;

    public ResMiningDailyTasks(int code) { super(code); }
}
