package com.jjg.game.season.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.REQ_SEASON_TRIAL_CHALLENGE)
@ProtoDesc("发起试炼挑战 (免费, 可重复发起=重试)")
public class ReqSeasonTrialChallenge extends AbstractMessage {
    @ProtoDesc("关卡序号 (1起)")
    public int trialId;
}
