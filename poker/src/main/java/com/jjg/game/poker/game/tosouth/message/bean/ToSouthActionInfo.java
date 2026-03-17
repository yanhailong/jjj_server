package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
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
    @ProtoDesc("推荐出牌组合列表 (仅针对当前操作玩家，其他玩家为null)")
    public List<ToSouthRecommendCards> recommendCardsList;
    @ProtoDesc("在本轮中玩家基本信息")
    public List<ToSouthCurRoundPlayerInfo> curRoundPlayerInfos = new ArrayList<>();
    @ProtoDesc("在本轮中已打出的牌历史记录")
    public List<ToSouthPlayCardRecord> curRoundPlayedCardHistory = new ArrayList<>();
    @ProtoDesc("上一手出的牌,pass则为空")
    public List<Integer> lastPlayCards;
    @ProtoDesc("上一手出的牌类型  1 单张  2 对子  3 三张  4 顺子  5 连对 6 炸弹")
    public int lastPlayCardsType;
    @ProtoDesc("上一手出牌 玩家座位 ID")
    public int lastPlaySeatId;
    @ProtoDesc("当前轮领打玩家座位 ID")
    public int roundLeaderSeatId;
    @ProtoDesc("是否是首轮 (需出黑桃3)")
    public boolean isFirstRound;
    @ProtoDesc("当前玩家的手牌")
    public List<Integer> selfHandCards;
    @ProtoDesc("当前玩家手牌中需要高亮的牌(2、炸弹、连对)")
    public List<Integer> selfHighlightCards;
    @ProtoDesc("刚刚过牌的玩家座位ID，0表示本次通知不是由过牌触发")
    public long  lastpassUserId;
}
