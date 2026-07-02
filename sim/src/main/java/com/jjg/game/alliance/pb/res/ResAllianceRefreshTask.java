package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.struct.AllianceTaskInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 *
 * @author 11
 * @date 2026/6/25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_REFRESH_TASK, resp = true)
@ProtoDesc("刷新任务")
public class ResAllianceRefreshTask extends AbstractResponse {
    @ProtoDesc("任务池(未被接取)")
    public List<AllianceTaskInfo> poolTasks;
    @ProtoDesc("我接取的任务(null=未接取)")
    public AllianceTaskInfo myTask;
    @ProtoDesc("下次整点补齐时间(ms)")
    public long nextRefreshTime;
    @ProtoDesc("今日已完成次数")
    public int dailyFinished;
    @ProtoDesc("每日完成次数上限")
    public int dailyLimit;
    @ProtoDesc("放弃冷却截止(ms)")
    public long abandonCdUntil;
    @ProtoDesc("今日刷新任务次数")
    public int todayRefreshTaskCount;

    public ResAllianceRefreshTask(int code) {
        super(code);
    }
}
