package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/7/18 9:43
 */
@ProtobufMessage
@ProtoDesc("奖池信息")
public class WealthBankPoolInfo {
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
