package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("图标中奖信息")
public class WolfMoonIconInfo {
    @ProtoDesc("中奖坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖图标id")
    public List<Integer> winIcons;
}
