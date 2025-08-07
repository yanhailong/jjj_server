package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/7 10:16
 */
@ProtobufMessage
@ProtoDesc("德州记录信息")
public class TexasHistory {
    @ProtoDesc("对局id")
    public long id;
    @ProtoDesc("小盲")
    public long SBValue;
    @ProtoDesc("大盲")
    public long BBValue;
    @ProtoDesc("第二轮公牌前端id")
    public List<Integer> preFlop;
    @ProtoDesc("第三轮公牌前端id")
    public int thirdCardId;
    @ProtoDesc("第四轮公牌前端id")
    public int fourthCardId;
    @ProtoDesc("本轮总下注的值")
    public List<TexasHistoryPlayerInfo> totalPlayerBetInfo;
    @ProtoDesc("轮次信息 从1开始 -1是摊牌")
    public List<TexasHistoryRoundInfo> texasHistoryRoundInfos;
}
