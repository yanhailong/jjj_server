package com.jjg.game.poker.game.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/25 14:36
 */
@ProtobufMessage
@ProtoDesc("玩家结算信息")
public class PokerPlayerSettlementInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("获得金币数(负数为失去)")
    public long getGold;
    @ProtoDesc("当前金币数")
    public long currentGold;
    @ProtoDesc("是否获胜")
    public boolean win;
}
