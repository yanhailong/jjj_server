package com.jjg.game.activity.buyOneGetSeven.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_BUY_ONE_GET_SEVEN_CLAIM_REWARDS)
@ProtoDesc("买一送七领取奖励")
public class ReqBuyOneGetSevenClaimRewards extends AbstractMessage {
    public int detailId;
}
