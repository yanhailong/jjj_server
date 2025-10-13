package com.jjg.game.activity.officialawards.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/4 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_OFFICIAL_AWARDS_JOIN_ACTIVITY, resp = true)
@ProtoDesc("官方派奖参与活动")
public class ResOfficialAwardsJoinActivity extends AbstractResponse {
    @ProtoDesc("领取奖励信息")
    public ItemInfo infoList;
    @ProtoDesc("奖励id")
    public int rewardDetailId;
    @ProtoDesc("剩余积分")
    public int remainPoint;
    @ProtoDesc("总奖池")
    public int totalPool;
    public ResOfficialAwardsJoinActivity(int code) {
        super(code);
    }
}
