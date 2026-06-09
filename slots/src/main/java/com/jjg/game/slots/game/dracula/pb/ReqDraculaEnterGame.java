package com.jjg.game.slots.game.dracula.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.slots.game.dracula.DraculaConstant;

/**
 * @author 11
 * @date 2025/8/1 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DRACULA_TYPE, cmd = DraculaConstant.MsgBean.REQ_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqDraculaEnterGame extends AbstractMessage {
}
