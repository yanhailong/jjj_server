package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 捐献档位项。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("捐献档位")
public class AllianceDonateInfo {
    @ProtoDesc("道具id")
    public int itemId;
    @ProtoDesc("道具数量")
    public List<Long> itemCounts;
    @ProtoDesc("奖励贡献值")
    public int rewardContribution;
    @ProtoDesc("用户每日捐献的次数")
    public int memberDailyLimit;
    @ProtoDesc("奖励声誉值")
    public int rewardReputation;
}
