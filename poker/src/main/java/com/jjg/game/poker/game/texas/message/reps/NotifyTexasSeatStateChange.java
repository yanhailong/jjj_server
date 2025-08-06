package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

/**
 * @author lm
 * @date 2025/7/26 14:42
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE
        , cmd = TexasConstant.MsgBean.NOTIFY_SEAT_STATE_CHANGE, resp = true)
@ProtoDesc("座位状态变化")
public class NotifyTexasSeatStateChange extends AbstractNotice {
    @ProtoDesc("玩家信息")
    public PokerPlayerInfo playerChange;
}
