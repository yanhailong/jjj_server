package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 16:27
 */
@ProtobufMessage
@ProtoDesc("推广分享周榜排行榜信息")
public class SharePromoteWeekRankInfo extends SharePromoteRankInfo{
    @ProtoDesc("奖励信息")
    public List<ItemInfo> itemInfos;
}
