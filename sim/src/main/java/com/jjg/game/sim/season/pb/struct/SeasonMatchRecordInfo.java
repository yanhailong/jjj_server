package com.jjg.game.sim.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import java.util.List;

@ProtoDesc("赛季异步对局记录")
public class SeasonMatchRecordInfo {
    @ProtoDesc("对局ID")
    public String matchId;
    @ProtoDesc("对手玩家ID")
    public long opponentId;
    @ProtoDesc("游戏ID")
    public int gameType;
    @ProtoDesc("下注赛季币")
    public long stake;
    @ProtoDesc("自己的逐局收益")
    public List<Long> playerSpinWins;
    @ProtoDesc("对手的逐局收益")
    public List<Long> opponentSpinWins;
    @ProtoDesc("结果：-1负，0平，1胜")
    public int result;
    @ProtoDesc("赛季币变化")
    public long coinChange;
    @ProtoDesc("完成时间，毫秒")
    public long finishTime;
}
