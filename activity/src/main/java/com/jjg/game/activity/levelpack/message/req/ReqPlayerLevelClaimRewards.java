package com.jjg.game.activity.levelpack.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/17 19:01
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = ActivityConstant.MsgBean.REQ_PLAYER_LEVEL_CLAIM_REWARDS)
@ProtoDesc("领取等级礼包奖励")
public class ReqPlayerLevelClaimRewards {
    @ProtoDesc("礼包id")
    public int id;
}
