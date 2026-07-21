package com.jjg.game.poker.game.douxian.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 请求确认本回合出牌(所有已开放区域摆满后可点击)，DESIGN.md 8.6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.REQ_DOU_XIAN_CONFIRM_PLAY)
@ProtoDesc("斗仙牌请求确认出牌")
public class ReqDouXianConfirmPlay extends AbstractMessage {
}
