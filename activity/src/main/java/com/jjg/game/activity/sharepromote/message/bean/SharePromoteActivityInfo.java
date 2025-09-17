package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("推广分享类型活动信息")
public class SharePromoteActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<SharePromoteDetailInfo> detailInfos;
    @ProtoDesc("进度")
    public long progress;
    @ProtoDesc("可领取收益")
    public long getProfitReward;
}
