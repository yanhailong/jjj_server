package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 请求刷新多人任务列表 (每日首次免费, 之后消耗道具; 已领取任务不刷走)。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_COOP_TASK_REFRESH)
@ProtoDesc("请求刷新多人任务列表")
public class ReqCoopTaskRefresh extends AbstractMessage {
}
