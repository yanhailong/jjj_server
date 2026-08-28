package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 数据看板游客品质刷新概率。
 */
@ProtobufMessage
@ProtoDesc("游客品质刷新概率")
public class OperationVisitorQualityRate {
    @ProtoDesc("游客品质，对应VisitorQuest.Quality")
    public int quality;
    @ProtoDesc("当前知名度计算后的品质总权重")
    public long weight;
    @ProtoDesc("刷新概率，万分比，10000表示100.00%")
    public int rate;
}
