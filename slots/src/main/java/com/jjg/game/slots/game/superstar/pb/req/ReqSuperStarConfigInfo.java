package com.jjg.game.slots.game.superstar.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superstar.SuperStarConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_STAR_TYPE, cmd = SuperStarConstant.MsgBean.REQ_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqSuperStarConfigInfo extends AbstractMessage {
}
