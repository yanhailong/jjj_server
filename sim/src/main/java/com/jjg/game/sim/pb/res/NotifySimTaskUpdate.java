package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * 通知任务更新 (进度/状态变更, 含领奖后激活的新节点)。
 *
 * @author 11
 * @date 2026/6/25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_SIM_TASK_UPDATE, resp = true)
@ProtoDesc("通知任务更新")
public class NotifySimTaskUpdate extends AbstractResponse {
    @ProtoDesc("变更的任务节点列表")
    public List<Task> tasks;

    public NotifySimTaskUpdate(int code) {
        super(code);
    }
}
