package com.jjg.game.hall.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/14 9:56
 */
@ProtobufMessage
@ProtoDesc("道具")
public class ItemInfo {
    @ProtoDesc("道具id")
    public int itemId;
    @ProtoDesc("数量")
    public long count;
}
