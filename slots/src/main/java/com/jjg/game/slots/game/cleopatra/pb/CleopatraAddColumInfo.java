package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/11 16:04
 */
@ProtobufMessage
@ProtoDesc("新增列信息")
public class CleopatraAddColumInfo {
    @ProtoDesc("新增列图标")
    public List<Integer> icons;
    @ProtoDesc("中奖图标信息")
    public List<CleopatraWinIconInfo> winIconInfoList;
    @ProtoDesc("倍率")
    public int times;
}
