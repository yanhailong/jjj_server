package com.jjg.game.slots.game.cleopatra.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/16 10:29
 */
@ProtobufMessage
@ProtoDesc("中奖图标信息")
public class CleopatraWinIconInfo {
    @ProtoDesc("图标id")
    public int iconId;
    @ProtoDesc("坐标")
    public List<Integer> indexList;
}
