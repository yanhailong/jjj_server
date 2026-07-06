package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("消除/补齐/神秘转换信息")
public class SuperGolfCascade {
    @ProtoDesc("本 cascade 的中奖图标信息")
    public SuperGolfIconInfo rewardIconInfo;
    @ProtoDesc("补齐的图标 坐标->图标id")
    public List<KVInfo> addIconInfos;
    @ProtoDesc("本 cascade 触发的'带框中奖变神秘'坐标集（无则空）")
    public List<Integer> turnToMysteryIndexes;
    @ProtoDesc("本 cascade 触发'全部神秘转随机符号'时的目标符号id；0=未触发")
    public int mysteryConvertedToIcon;
    @ProtoDesc("本 cascade 应用的乘倍值（仅在 mystery 触发的兑奖 cascade 上 > 1）")
    public int multiplier;
}
