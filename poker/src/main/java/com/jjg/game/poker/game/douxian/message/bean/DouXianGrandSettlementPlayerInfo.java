package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 大结算(第4回合结束)单个玩家的分回合输赢明细，DESIGN.md 8.12
 */
@ProtobufMessage
@ProtoDesc("斗仙牌大结算玩家信息")
public class DouXianGrandSettlementPlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("每回合输赢(下标0对应第1回合)")
    public List<Long> roundChangeList;
    @ProtoDesc("总输赢")
    public long totalChange;
    @ProtoDesc("是否中途认输")
    public boolean conceded;
}
