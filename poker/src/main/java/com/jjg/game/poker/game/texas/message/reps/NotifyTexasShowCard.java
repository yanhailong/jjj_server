package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.texas.constant.TexasConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/1 09:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE, cmd = TexasConstant.MsgBean.NOTIFY_SHOW_CARD, resp = true)
@ProtoDesc("请求亮牌")
public class NotifyTexasShowCard extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("牌")
    public List<Integer> cards;
    @ProtoDesc("牌型")
    public int handType;
}
