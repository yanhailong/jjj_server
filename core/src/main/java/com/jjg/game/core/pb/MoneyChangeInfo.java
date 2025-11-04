package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/11/3 17:03
 */
@ProtobufMessage
@ProtoDesc("玩家货币变化")
public class MoneyChangeInfo {
    @ProtoDesc("货币类型  98.钻石  99.金币")
    public int moneyType;
    @ProtoDesc("变化值  正为增加  负为减少")
    public long changeValue;
    @ProtoDesc("变化后的值")
    public long afterValue;
}
