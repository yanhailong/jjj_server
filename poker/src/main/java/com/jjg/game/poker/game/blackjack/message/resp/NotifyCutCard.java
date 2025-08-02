package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/29 10:04
 */
@ProtobufMessage
@ProtoDesc("通知玩家分牌结果")
public class NotifyCutCard extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("第一副牌")
    public List<Integer> firstList;
    @ProtoDesc("第二副牌")
    public List<Integer> secondList;
}
