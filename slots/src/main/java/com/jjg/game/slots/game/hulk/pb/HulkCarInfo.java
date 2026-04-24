package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/4/23
 */
@ProtobufMessage
@ProtoDesc("汽车信息")
public class HulkCarInfo {
    @ProtoDesc("标记")
    public int index;
    @ProtoDesc("中奖金额")
    public long winGold;
}
