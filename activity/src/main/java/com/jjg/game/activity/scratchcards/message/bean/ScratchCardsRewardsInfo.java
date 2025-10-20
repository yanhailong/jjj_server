package com.jjg.game.activity.scratchcards.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/20 09:47
 */
@ProtoDesc("刮刮卡奖励信息")
@ProtobufMessage
public class ScratchCardsRewardsInfo {
    @ProtoDesc("领取奖励信息")
    public List<ItemInfo> infoList;
    @ProtoDesc("含7数")
    public int numOf7;
    @ProtoDesc("中奖次数")
    public int times;
}
