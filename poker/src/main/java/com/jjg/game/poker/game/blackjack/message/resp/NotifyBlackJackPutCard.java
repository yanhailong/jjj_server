package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

/**
 * @author lm
 * @date 2025/8/6 16:22
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE,cmd = BlackJackConstant.MsgBean.NOTIFY_BLACKJACK_PUT_CARD,resp = true)
@ProtoDesc("通知玩家拿牌信息")
public class NotifyBlackJackPutCard extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("牌id")
    public int cardId;
    @ProtoDesc("总点数")
    public int totalPoint;
    @ProtoDesc("下一个操作人")
    public long operationId;
    @ProtoDesc("操作结束时间")
    public long overTime;
}
