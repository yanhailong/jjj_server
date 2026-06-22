package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceTaskInfo;

import java.util.List;

/**
 * 联盟任务列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_TASK_LIST, resp = true)
@ProtoDesc("联盟任务列表返回")
public class ResAllianceTaskList extends AbstractResponse {
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

    public ResAllianceTaskList(int code) {
        super(code);
    }
}
