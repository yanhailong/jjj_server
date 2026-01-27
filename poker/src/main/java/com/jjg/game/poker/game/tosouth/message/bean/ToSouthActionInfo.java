package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;
import java.util.Set;

@ProtobufMessage
@ProtoDesc("南方前进牌桌操作状态信息")
public class ToSouthActionInfo {
    @ProtoDesc("等待操作用户 id")
    public long waitPlayerId;
    @ProtoDesc("等待结束时间")
    public long waitEndTime;
    @ProtoDesc("是否能 pass")
    public boolean canPass = true;
    @ProtoDesc("是否能 play")
    public boolean canPlay = true;
    @ProtoDesc("本轮中已pass的玩家座位，不能再出牌")
    public Set<Integer> curRoundPassedPlayerSeats;
    @ProtoDesc("推荐出牌组合列表 (仅针对当前操作玩家，其他玩家为null)")
    public List<ToSouthRecommendCards> recommendCardsList;
    
    @ProtoDesc("上一手出的牌")
    public List<Integer> lastPlayCards;
    @ProtoDesc("上一手出牌玩家座位 ID")
    public int lastPlaySeatId;
    @ProtoDesc("当前轮领打玩家座位 ID")
    public int roundLeaderSeatId;
    @ProtoDesc("是否是首轮 (需出黑桃3)")
    public boolean isFirstRound;
    @ProtoDesc("当前玩家的手牌")
    public List<Integer> selfHandCards;
}
