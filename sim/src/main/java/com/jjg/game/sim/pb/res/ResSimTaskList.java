package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * 任务列表返回。任务静态信息(图标/描述/奖励/跳转)客户端依配置 id 自取, 服务端只下发进度与状态。
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
    @ProtoDesc("成就当前节点列表 (每个成就组一条)")
    public List<Task> achievementTasks;
    @ProtoDesc("已激活的成就勋章配置id")
    public List<Integer> activatedMedalIds;
    @ProtoDesc("当前展示的成就勋章配置id, 顺序即展示顺序")
    public List<Integer> displayedMedalIds;

    public ResSimTaskList(int code) {
        super(code);
    }
}
