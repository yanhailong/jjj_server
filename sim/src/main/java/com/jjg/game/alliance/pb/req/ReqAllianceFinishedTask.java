package com.jjg.game.alliance.pb.req;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 获取已完成的联盟任务。
 *
 * @author 11
 * @date 2026/6/26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.REQ_FINISHED_TASK)
@ProtoDesc("获取已完成的联盟任务")
public class ReqAllianceFinishedTask extends AbstractMessage {
}
