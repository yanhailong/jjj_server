package com.jjg.game.slots.game.wolfmoon.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WOLF_MOON, cmd = WolfMoonConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqWolfMoonStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
