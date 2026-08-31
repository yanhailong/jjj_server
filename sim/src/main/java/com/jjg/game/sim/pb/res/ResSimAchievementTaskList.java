package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_ACHIEVEMENT_TASK_LIST, resp = true)
@ProtoDesc("成就任务列表返回")
public class ResSimAchievementTaskList extends AbstractResponse {
    @ProtoDesc("指定建筑下的成就任务列表，按任务配置id升序")
    public List<Task> achievementTasks;

    public ResSimAchievementTaskList(int code) {
        super(code);
    }
}
