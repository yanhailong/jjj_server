package com.jjg.game.slots.game.garaGemstone3.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("中奖图标信息")
public class GaraGemstone3WinIconInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条线上中奖图标的坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("中奖金币（含分裂倍数，baseTimes * splitTimes * oneBetScore）")
    public long winGold;
    @ProtoDesc("原始中奖金币（不含分裂倍数，baseTimes * oneBetScore）")
    public long baseWinGold;
}
