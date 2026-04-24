package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hulk.HulkConstant;

/**
 * @author 11
 * @date 2026/4/23
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HULK, cmd = HulkConstant.MsgBean.REQ_MINI_AIRPLANE)
@ProtoDesc("请求飞机小游戏")
public class ReqHulkMiniGameAirPlane extends AbstractMessage {
}
