package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("玩家当前赛季信息")
public class SeasonInfo {
    @ProtoDesc("赛季配置ID")
    public int seasonId;
    @ProtoDesc("阶段：1新手，2进阶，3循环")
    public int phase;
    @ProtoDesc("循环赛季序号，前置赛季为0")
    public int cycleIndex;
    @ProtoDesc("赛季名称多语言ID")
    public int nameLanguageId;
    @ProtoDesc("开始时间，毫秒")
    public long startTime;
    @ProtoDesc("结束时间，毫秒")
    public long endTime;
    @ProtoDesc("当前赛季内天数，从1开始")
    public int day;
    @ProtoDesc("赛季开放的游戏ID")
    public int gameType;
    @ProtoDesc("当前赛季币")
    public long seasonCoin;
    @ProtoDesc("本赛季累计获得赛季币")
    public long totalEarnedCoin;
    @ProtoDesc("当前段位配置ID，0表示无段位")
    public int tierId;
    @ProtoDesc("今日匹配次数")
    public int dailyMatchCount;
    @ProtoDesc("今日匹配赢取赛季币")
    public long dailyWinAmount;
    @ProtoDesc("今日匹配损失赛季币")
    public long dailyLossAmount;
    @ProtoDesc("当前排行榜名次")
    public int rank;
    @ProtoDesc("试炼关卡ID到最高星级；无关卡配置时为空")
    public List<KVInfo> trialStars;
}
