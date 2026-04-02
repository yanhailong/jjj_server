package com.jjg.game.ploy.games.highlowpoker.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/4/1 15:07
 */
@ProtobufMessage
@ProtoDesc("历史信息")
public class HighLowHistoryInfo {
    @ProtoDesc("牌id")
    public int cardId;
    @ProtoDesc("赔率")
    public String odd;
}
