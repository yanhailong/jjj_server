package com.jjg.game.table.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/8 18:25
 */
@ProtobufMessage
@ProtoDesc("玩家筹码皮肤id")
public class PlayerChip {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("筹码皮肤id")
    public int chipId;
}
