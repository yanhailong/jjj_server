package com.jjg.game.slots.game.wealthgod.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 *
 */
@ProtobufMessage
@ProtoDesc("每条中奖线信息")
public class WealthGodResultLineInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条线上中奖图标的坐标")
    public List<Integer> iconIndex;
    @ProtoDesc("倍率")
    public int times;
    @ProtoDesc("中奖金币")
    public long winGold;
}
