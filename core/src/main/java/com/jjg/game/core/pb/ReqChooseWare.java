package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_CHOOSE_WARE)
@ProtoDesc("选择游戏场次进入")
public class ReqChooseWare extends AbstractMessage {

    @ProtoDesc("游戏类型")
    public int gameType;

    @ProtoDesc("场次id")
    public int wareId;
}
