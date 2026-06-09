package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("中奖图标信息")
public class BountyDuelIconInfo {
    @ProtoDesc("中奖图标位置")
    public List<Integer> iconIndexs;
    @ProtoDesc("金框图标转 wild 的位置")
    public List<Integer> replaceWildIndexs;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖图标ID")
    public List<Integer> winIcons;
}
