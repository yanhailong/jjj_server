package com.jjg.game.slots.game.pegasusunbridle.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;

/**
 * @author 11
 * @date 2025/8/1 17:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = PegasusUnbridleConstant.MsgBean.REQ_PEGASUS_UNBRIDLE_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqPegasusUnbridleStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
