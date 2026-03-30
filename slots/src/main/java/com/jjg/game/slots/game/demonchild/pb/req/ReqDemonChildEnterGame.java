package com.jjg.game.slots.game.demonchild.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;

/**
 * @author 11
 * @date 2025/8/1 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = DemonChildConstant.MsgBean.REQ_DEMON_CHILD_ENTER_GAME)
@ProtoDesc("请求配置信息")
public class ReqDemonChildEnterGame extends AbstractMessage {
}
