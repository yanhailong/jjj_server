package com.jjg.game.slots.game.frozenThrone.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FROZEN_THRONE, cmd = FrozenThroneConstant.MsgBean.REQ_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqFrozenThroneEnterGame extends AbstractMessage {
}
