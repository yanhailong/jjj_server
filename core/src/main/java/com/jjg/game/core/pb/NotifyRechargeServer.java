package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/9/22 20:41
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
        cmd = MessageConst.ToServer.NOTIFY_PLAYER_RECHARGE, resp = true, toPbFile = false)
@ProtoDesc("通知玩家升级奖励")
public class NotifyRechargeServer extends AbstractMessage {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("订单id")
    public String orderId;
}
