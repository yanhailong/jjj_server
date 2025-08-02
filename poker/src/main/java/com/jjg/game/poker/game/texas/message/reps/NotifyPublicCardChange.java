package com.jjg.game.poker.game.texas.message.reps;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.message.bean.TexasRoundInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/30 11:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TEXAS_TYPE
        , cmd = TexasConstant.MsgBean.NOTIFY_PUBLIC_CARD_CHANGE, resp = true)
@ProtoDesc("通知公区扑克牌变化")
public class NotifyPublicCardChange extends AbstractNotice {
    @ProtoDesc("执行的玩家id")
    public long playerId;
    @ProtoDesc("超时时间")
    public long overTime;
    @ProtoDesc("轮次信息")
    public TexasRoundInfo roundInfo;

}
