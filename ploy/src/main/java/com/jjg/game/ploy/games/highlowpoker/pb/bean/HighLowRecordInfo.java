package com.jjg.game.ploy.games.highlowpoker.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowHistoryInfo;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 15:07
 */
@ProtobufMessage
@ProtoDesc("历史信息")
public class HighLowRecordInfo {
    @ProtoDesc("牌信息")
    public List<HighLowHistoryInfo> historyInfos;
    @ProtoDesc("总营收")
    public long totalIncome;
    @ProtoDesc("税收")
    public long tax;
}
