package com.jjg.game.ploy.games.hillo.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("HILLO record")
public class HilloRecordInfo {
    @ProtoDesc("round history")
    public List<HilloHistoryInfo> historyInfos;
    @ProtoDesc("total income")
    public long totalIncome;
    @ProtoDesc("round start time")
    public long startTime;
    @ProtoDesc("bet amount")
    public long bet;
    @ProtoDesc("bet mode (0 manual, 1 auto)")
    public int betMode;
    @ProtoDesc("balance after settle")
    public long balanceAfter;
}
