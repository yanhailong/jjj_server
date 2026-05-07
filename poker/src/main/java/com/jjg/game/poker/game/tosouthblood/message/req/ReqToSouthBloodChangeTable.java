package com.jjg.game.poker.game.tosouthblood.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;

/**
 * @author lm
 * @date 2025/8/5 13:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_BLOOD, cmd = ToSouthBloodConstant.MsgBean.REQ_CHANGE_TABLE)
@ProtoDesc("请求换桌")
public class ReqToSouthBloodChangeTable extends AbstractMessage {
}
