package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/1 10:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant
        .MsgBean.NOTIFY_PLAYER_CHANGE, resp = true)
@ProtoDesc("德州通知结算后通知玩家变化")
public class NotifyTexasSettlementPlayerChange extends AbstractNotice {
    @ProtoDesc("玩家变化列表")
    public List<PokerPlayerInfo> pokerPlayerInfos;
}
