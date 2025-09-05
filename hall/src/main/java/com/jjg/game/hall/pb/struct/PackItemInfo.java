package com.jjg.game.hall.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/11 14:55
 */
@ProtobufMessage
@ProtoDesc("背包中的道具")
public class PackItemInfo {
    @ProtoDesc("格子id")
    public int girdId;
    @ProtoDesc("道具信息")
    public ItemInfo item;
}
