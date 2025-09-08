package com.jjg.game.activity.privilegecard.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.privilegecard.message.bean.PrivilegeCardDetailInfo;
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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_PRIVILEGE_CARD_CLAIM_REWARDS,resp = true)
@ProtoDesc("响应每日奖金领取活动奖励")
public class ResPrivilegeCardClaimRewards extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public long detailId;
    @ProtoDesc("领取奖励信息")
    public List<ItemInfo> infoList;
    @ProtoDesc("详细信息")
    public PrivilegeCardDetailInfo detailInfo;

    public ResPrivilegeCardClaimRewards(int code) {
        super(code);
    }
}
