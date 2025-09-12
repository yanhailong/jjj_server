package com.jjg.game.slots.game.wealthgod.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.REQ_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqWealthGodConfigInfo extends AbstractMessage {
}
