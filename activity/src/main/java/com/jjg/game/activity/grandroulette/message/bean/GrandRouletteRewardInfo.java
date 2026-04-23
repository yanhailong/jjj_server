package com.jjg.game.activity.grandroulette.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("大转盘奖励信息")
public class GrandRouletteRewardInfo {
    @ProtoDesc("索引id")
    public long index;
    @ProtoDesc("奖励下限")
    public ItemInfo rewardMin;
    @ProtoDesc("奖励上限限")
    public ItemInfo rewardMax;
}
