package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.NOTIFY_SEASON_TRIAL_RESULT, resp = true)
@ProtoDesc("通知试炼挑战结算 (窗口局数耗尽或提前满星)")
public class NotifySeasonTrialResult extends AbstractResponse {
    @ProtoDesc("关卡序号")
    public int trialId;
    @ProtoDesc("本次达成星级 (0=挑战失败)")
    public int achievedStars;
    @ProtoDesc("历史最高星级 (结算后)")
    public int bestStars;
    @ProtoDesc("本次新发放的奖励 (仅新突破的星级, 无则为空)")
    public List<ItemInfo> rewards;
    @ProtoDesc("本次挑战用掉的局数")
    public int spinCount;
    @ProtoDesc("本次挑战最终进度值")
    public long progress;
    @ProtoDesc("结算后的赛季币")
    public long seasonCoin;

    public NotifySeasonTrialResult(int code) {
        super(code);
    }
}
