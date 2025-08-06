package com.jjg.game.poker.game.texas.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/8/1 09:32
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE,cmd = TexasConstant.MsgBean.REQ_SHOW_CARD)
@ProtoDesc("请求亮牌")
public class ReqTexasShowCard extends AbstractMessage {

}
