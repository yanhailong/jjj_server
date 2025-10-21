package com.jjg.game.activity.levelpack.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("等级礼包详情信息")
public class PlayerLevelPackDetailInfo {
    @ProtoDesc("配置id")
    public int id;
    @ProtoDesc("领取状态 1不可领取 2可领取 3已领取")
    public int claimStatus;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewardItems;
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("礼包购买金额")
    public String buyPrice;
}
