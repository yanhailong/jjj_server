package com.jjg.game.poker.game.common.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.common.constant.PokerConstant;

/**
 * @author lm
 * @date 2025/7/30 10:23
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.NOTIFY_SAMPLE_CARD_OPERATION, resp = true)
public class NotifySampleCardOperation extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("操作类型")
    public int operationType;
    @ProtoDesc("下一个玩家id")
    public long nextPlayerId;
    @ProtoDesc("超时时间戳")
    public long overTime;
}
