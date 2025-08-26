package com.jjg.game.poker.game.common.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.common.constant.PokerConstant;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;

/**
 * @author lm
 * @date 2025/7/26 11:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE,
        cmd = PokerConstant.MsgBean.NOTIFY_PLAYER_CHANGE, resp = true)
@ProtoDesc("通知玩家变化")
public class NotifyPokerPlayerChange extends AbstractNotice {
    @ProtoDesc("玩家基本信息")
    public PokerPlayerInfo pokerPlayerInfo;
    @ProtoDesc("房间总人数")
    public long totalNum;
}
