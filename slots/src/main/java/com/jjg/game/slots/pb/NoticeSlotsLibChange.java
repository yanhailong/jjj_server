package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/7/23 17:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE, cmd = SlotsConst.MsgBean.RES_START_GAME,resp = true,toPbFile = false)
@ProtoDesc("通知slots节点，lib库变化")
public class NoticeSlotsLibChange {
    @ProtoDesc("游戏类型")
    public int gameType;
}
