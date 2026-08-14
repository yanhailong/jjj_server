package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 领取任务奖励返回。
 *
 * @author 11
 * @date 2026/6/25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_TASK_REWARD, resp = true)
@ProtoDesc("领取任务奖励返回")
public class ResSimTaskReward extends AbstractResponse {
    @ProtoDesc("已领取的任务配置id")
    public int taskId;
    @ProtoDesc("领奖后新激活的下一节点 (链结束时为空)")
    public Task nextTask;

    public ResSimTaskReward() {
        super(0);
    }

    public ResSimTaskReward(int code) {
        super(code);
    }
}
