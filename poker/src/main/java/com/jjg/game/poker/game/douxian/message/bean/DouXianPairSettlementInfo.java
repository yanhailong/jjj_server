package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 两名玩家之间一整轮(凡界->灵界->仙界)的结算结果，DESIGN.md 四.3 全胜规则
 */
@ProtobufMessage
@ProtoDesc("斗仙牌两两结算结果")
public class DouXianPairSettlementInfo {
    @ProtoDesc("玩家A")
    public long playerAId;
    @ProtoDesc("玩家B")
    public long playerBId;
    @ProtoDesc("常规三区域结算结果，按凡->灵->仙顺序")
    public List<DouXianZoneSettlementInfo> zoneResults;
    @ProtoDesc("全胜方玩家id，0表示本轮未触发全胜")
    public long grandWinPlayerId;
    @ProtoDesc("全胜二次结算结果，按凡->灵->仙顺序，未触发时为空")
    public List<DouXianZoneSettlementInfo> grandWinExtraResults;
}
