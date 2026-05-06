package com.jjg.game.activity.grandroulette.message.res;

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
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_GRAND_ROULETTE_CLAIM_REWARDS, resp = true)
@ProtoDesc("大转盘领取活动奖励")
public class ResGrandRouletteClaimRewards extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("领取奖励信息")
    public ItemInfo infoList;

    public ResGrandRouletteClaimRewards(int code) {
        super(code);
    }
}
