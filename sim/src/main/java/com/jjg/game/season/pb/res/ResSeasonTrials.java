package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.struct.SeasonTrialInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_TRIALS, resp = true)
@ProtoDesc("试炼任务列表 (仅新手赛季, 其他阶段为空)")
public class ResSeasonTrials extends AbstractResponse {
    @ProtoDesc("关卡列表")
    public List<SeasonTrialInfo> trials;
    @ProtoDesc("进行中的挑战关卡序号 (0=无)")
    public int activeTrialId;

    public ResSeasonTrials(int code) {
        super(code);
    }
}
