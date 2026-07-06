package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CoopTaskInfo;

/**
 * 多人任务状态变更通知 (结算成功/失败、房间失效自愈回退时推送)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.NOTIFY_COOP_TASK_UPDATE, resp = true)
@ProtoDesc("多人任务状态变更通知")
public class NotifyCoopTaskUpdate extends AbstractResponse {
    @ProtoDesc("变更后的任务信息")
    public CoopTaskInfo task;

    public NotifyCoopTaskUpdate(int code) {
        super(code);
    }
}
