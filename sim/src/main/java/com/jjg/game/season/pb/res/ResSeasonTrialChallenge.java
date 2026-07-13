package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_TRIAL_CHALLENGE, resp = true)
@ProtoDesc("发起试炼挑战结果")
public class ResSeasonTrialChallenge extends AbstractResponse {
    @ProtoDesc("关卡序号")
    public int trialId;
    @ProtoDesc("挑战窗口总局数")
    public int expectedSpins;

    public ResSeasonTrialChallenge(int code) {
        super(code);
    }
}
