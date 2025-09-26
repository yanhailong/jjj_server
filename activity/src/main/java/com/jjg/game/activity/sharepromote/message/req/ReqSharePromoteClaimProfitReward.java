package com.jjg.game.activity.sharepromote.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 15:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_CLAIM_PROFIT_REWARD)
@ProtoDesc("请求领取收益奖励")
public class ReqSharePromoteClaimProfitReward {
}
