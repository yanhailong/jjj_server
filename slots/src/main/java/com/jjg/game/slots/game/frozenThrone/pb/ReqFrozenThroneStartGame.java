package com.jjg.game.slots.game.frozenThrone.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;

/**
 * @author lihaocao
 * @date 2025/12/2 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.FROZEN_THRONE, cmd = FrozenThroneConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqFrozenThroneStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
