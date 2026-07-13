package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 请求取消托管，DESIGN.md 8.14
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_CANCEL_HOSTING)
@ProtoDesc("斗仙牌请求取消托管")
public class ReqDouXianCancelHosting extends AbstractMessage {
}
