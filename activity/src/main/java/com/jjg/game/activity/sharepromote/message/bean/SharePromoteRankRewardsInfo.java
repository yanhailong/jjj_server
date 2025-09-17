package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 16:33
 */
@ProtobufMessage
@ProtoDesc("推广分享排行奖励信息")
public class SharePromoteRankRewardsInfo {
    @ProtoDesc("最大排行")
    public int maxRank;
    @ProtoDesc("最小排行")
    public int minRank;
    @ProtoDesc("奖励信息")
    public List<ItemInfo> itemInfos;
}
