package com.jjg.game.activity.sharepromote.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 16:37
 */
@ProtobufMessage
@ProtoDesc("我的推广分享排行榜信息")
public class SharePromoteSelfRankInfo extends SharePromoteRankInfo {
    @ProtoDesc("充值总金额")
    public String totalRecharge;
}
