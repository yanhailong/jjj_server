package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SIM_ACHIEVEMENT_TASK_LIST)
@ProtoDesc("请求成就任务列表")
public class ReqSimAchievementTaskList extends AbstractMessage {
    @ProtoDesc("建筑id，0=全部成就")
    public int buildingId;
}
