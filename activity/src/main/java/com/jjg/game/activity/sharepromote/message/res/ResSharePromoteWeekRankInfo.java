package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteRankRewardsInfo;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteWeekRankInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 16:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_WEEK_RANK_INFO, resp = true)
@ProtoDesc("请求推广分享周榜信息")
public class ResSharePromoteWeekRankInfo extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("排行信息")
    public List<SharePromoteWeekRankInfo> rankInfoList;
    @ProtoDesc("排行奖励信息")
    public List<SharePromoteRankRewardsInfo> rankRewardsInfoList;
    @ProtoDesc("起始索引")
    public int startIndex;
    @ProtoDesc("是否还有数据")
    public boolean hasNext;
    @ProtoDesc("剩余时间起始索引为0时会发")
    public long remainTime;

    public ResSharePromoteWeekRankInfo(int code) {
        super(code);
    }
}
