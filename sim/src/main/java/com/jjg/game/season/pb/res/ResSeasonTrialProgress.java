package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_TRIAL_PROGRESS, resp = true)
@ProtoDesc("进行中的试炼进度")
public class ResSeasonTrialProgress extends AbstractResponse {
    @ProtoDesc("已完成的旋转次数")
    public int spinCount;
    @ProtoDesc("当前进度值")
    public long progress;

    public ResSeasonTrialProgress() {
        super(0);
    }

    public ResSeasonTrialProgress(int code) {
        super(code);
    }
}
