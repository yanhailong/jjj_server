package com.jjg.game.activity.sharepromote.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.sharepromote.message.bean.SharePromoteRewardsRecode;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/16 15:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_SHARE_PROMOTE_GLOBAL_INFO, resp = true)
@ProtoDesc("推广分享总览信息")
public class ResSharePromoteGlobalInfo extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("昨日总收益")
    public long yesterdayIncome;
    @ProtoDesc("历史总收益")
    public long historyIncome;
    @ProtoDesc("分享玩家数")
    public int sharePlayerNum;
    @ProtoDesc("我的邀请码")
    public String invitationCode;
    @ProtoDesc("领取记录")
    public List<SharePromoteRewardsRecode> recodes;
    @ProtoDesc("当前收益比")
    public int earningsRatio;

    public ResSharePromoteGlobalInfo(int code) {
        super(code);
    }
}
