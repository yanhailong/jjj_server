package com.jjg.game.poker.game.common.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.room.constant.EGamePhase;

/**
 * @author lm
 * @date 2025/7/26 14:57
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.NOTIFY_PHASE_CHANGE, resp = true)
@ProtoDesc("游戏大阶段变化")
public class NotifyPhaseChange extends AbstractNotice {
    @ProtoDesc("当前游戏阶段")
    public EGamePhase phase;
    @ProtoDesc("阶段结束时间（-1为一直持续）")
    public long endTime;

}
