package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * 任务列表返回。图标/描述/跳转等静态信息由客户端依配置 id 自取，服务端下发进度、状态与奖励道具。
 *
 * @author 11
 * @date 2026/6/25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_TASK_LIST, resp = true)
@ProtoDesc("任务列表返回")
public class ResSimTaskList extends AbstractResponse {
    @ProtoDesc("主线当前节点 (无主线配置时为空)")
    public Task mainTask;
    @ProtoDesc("主线是否全部完成 (任务预告状态)")
    public boolean mainFinished;
    @ProtoDesc("指定徽章下的成就任务列表，按任务配置id升序")
    public List<Task> achievementTasks;

    public ResSimTaskList(int code) {
        super(code);
    }
}
