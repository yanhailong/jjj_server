package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 领取多人任务返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COOP_TASK_CLAIM, resp = true)
@ProtoDesc("领取多人任务返回")
public class ResCoopTaskClaim extends AbstractResponse {
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("今日剩余领取次数")
    public int remainClaimCount;

    public ResCoopTaskClaim(int code) {
        super(code);
    }
}
