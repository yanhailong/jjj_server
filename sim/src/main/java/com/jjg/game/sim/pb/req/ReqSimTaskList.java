package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 请求任务列表 (主线 + 成就一并返回)。
 *
 * @author 11
 * @date 2026/6/25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SIM_TASK_LIST)
@ProtoDesc("请求任务列表")
public class ReqSimTaskList extends AbstractMessage {
}
