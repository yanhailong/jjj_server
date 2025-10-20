package com.jjg.game.activity.dailylogin.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.dailylogin.message.bean.DailyLoginDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/4 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_DAILY_LOGIN_CLAIM_REWARDS,resp = true)
@ProtoDesc("响应每日签到领取活动奖励")
public class ResDailyLoginClaimRewards extends AbstractResponse {
    @ProtoDesc("领取奖励信息")
    public List<ItemInfo> infoList;
    @ProtoDesc("详细信息")
    public List<DailyLoginDetailInfo> detailInfo;

    public ResDailyLoginClaimRewards(int code) {
        super(code);
    }
}
