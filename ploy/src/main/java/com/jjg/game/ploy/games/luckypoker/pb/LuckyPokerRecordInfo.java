package com.jjg.game.ploy.games.luckypoker.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/20
 */
@ProtobufMessage
@ProtoDesc("游戏记录")
public class LuckyPokerRecordInfo {
    @ProtoDesc("最终手牌")
    public List<Integer> finalCardIds;
    @ProtoDesc("倍数")
    public int times;
}
