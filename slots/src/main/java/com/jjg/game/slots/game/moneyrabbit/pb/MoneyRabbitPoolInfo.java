package com.jjg.game.slots.game.moneyrabbit.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("奖池信息")
public class MoneyRabbitPoolInfo {
    @ProtoDesc("奖池id")
    public int id;
    @ProtoDesc("初始倍数")
    public int initTimes;
    @ProtoDesc("最大倍数")
    public int maxTimes;
    @ProtoDesc("变化间隔时间")
    public int perSomeSec;
    @ProtoDesc("每过单位时间，奖池变化万分比")
    public int updateProp;
}
