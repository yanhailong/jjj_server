package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/7 10:40
 */
@ProtobufMessage
@ProtoDesc("德州记录每轮信息")
public class TexasHistoryRoundInfo {
    public int roundIndex;
    public List<Long> potAllBet;
    public List<TexasHistoryPlayerInfo> roundInfo;

    public TexasHistoryRoundInfo(int roundIndex) {
        this.roundIndex = roundIndex;
    }
}
