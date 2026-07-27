package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 两名玩家在单个区域的结算结果，DESIGN.md 四.1/四.2
 */
@ProtobufMessage
@ProtoDesc("斗仙牌单区域结算结果")
public class DouXianZoneSettlementInfo {
    @ProtoDesc("区域(1凡界 2灵界 3仙界)")
    public int zoneId;
    @ProtoDesc("赢家玩家id")
    public long winnerId;
    @ProtoDesc("输家玩家id")
    public long loserId;
    @ProtoDesc("赢家灵力值")
    public long winnerAether;
    @ProtoDesc("输家灵力值")
    public long loserAether;
    @ProtoDesc("赢家牌型名称")
    public String winnerHandTypeName;
    @ProtoDesc("输家牌型名称")
    public String loserHandTypeName;
    @ProtoDesc("本次结算实际变化的金额(已按封顶/最小输赢裁剪)")
    public long changeValue;
    @ProtoDesc("赢家牌型多语言id(来自ImmortalHand.xlsx)")
    public int winnerHandTypeNameId;
    @ProtoDesc("输家牌型多语言id(来自ImmortalHand.xlsx)")
    public int loserHandTypeNameId;
}
