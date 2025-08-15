package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/7 10:07
 */
@ProtobufMessage
@ProtoDesc("倍场界面奖池信息")
public class WarePoolInfo {
    @ProtoDesc("场次ID")
    public int wareId;
    @ProtoDesc("奖池")
    public long pool;
}
