package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 领取多人任务奖励返回 (领取后任务从列表移除)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COOP_TASK_REWARD, resp = true)
@ProtoDesc("领取多人任务奖励返回")
public class ResCoopTaskReward extends AbstractResponse {
    @ProtoDesc("已领取的任务配置id")
    public int taskId;

    public ResCoopTaskReward(int code) {
        super(code);
    }
}
