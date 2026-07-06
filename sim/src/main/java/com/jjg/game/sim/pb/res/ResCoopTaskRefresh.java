package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.CoopTaskInfo;

import java.util.List;

/**
 * 刷新多人任务列表返回。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COOP_TASK_REFRESH, resp = true)
@ProtoDesc("刷新多人任务列表返回")
public class ResCoopTaskRefresh extends AbstractResponse {
    @ProtoDesc("刷新后的任务列表")
    public List<CoopTaskInfo> tasks;
    @ProtoDesc("今日免费刷新是否可用")
    public boolean freeRefresh;

    public ResCoopTaskRefresh(int code) {
        super(code);
    }
}
