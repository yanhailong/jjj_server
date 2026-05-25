package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * @author 11
 * @date 2026/5/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
@ProtoDesc("获取slots技能")
public class ReqSimGetSkills extends AbstractMessage {
}