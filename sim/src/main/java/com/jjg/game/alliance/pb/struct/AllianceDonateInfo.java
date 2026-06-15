package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 捐献档位项。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("捐献档位")
public class AllianceDonateInfo {
    @ProtoDesc("档位id")
    public int donateId;
    @ProtoDesc("消耗道具id(0=免费)")
    public int costItemId;
    @ProtoDesc("消耗数量")
    public long costCount;
    @ProtoDesc("奖励贡献值")
    public long rewardContribution;
    @ProtoDesc("给联盟的声誉值")
    public long rewardReputation;
    @ProtoDesc("本次是否免费(当日首次)")
    public boolean free;
}
