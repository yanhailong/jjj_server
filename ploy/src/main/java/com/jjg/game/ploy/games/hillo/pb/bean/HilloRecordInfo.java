package com.jjg.game.ploy.games.hillo.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloHistoryInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("HILLO 历史记录")
public class HilloRecordInfo {
    @ProtoDesc("单局过程记录")
    public List<HilloHistoryInfo> historyInfos;
    @ProtoDesc("本局总盈亏")
    public long totalIncome;
    @ProtoDesc("本局开始时间")
    public long startTime;
    @ProtoDesc("下注金额")
    public long bet;
    @ProtoDesc("下注模式，0=手动，1=自动")
    public int betMode;
    @ProtoDesc("结算后余额")
    public long balanceAfter;
}
