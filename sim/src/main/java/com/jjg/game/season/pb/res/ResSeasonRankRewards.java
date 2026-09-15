package com.jjg.game.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.RankRewardInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.season.constant.SeasonConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SEASON, cmd = SeasonConstant.MsgBean.RES_SEASON_RANK_REWARDS, resp = true)
@ProtoDesc("返回当前赛季排行榜奖励列表")
public class ResSeasonRankRewards extends AbstractResponse {
    @ProtoDesc("按起始排名升序排列的奖励区间")
    public List<RankRewardInfo> rewards;

    public ResSeasonRankRewards(int code) {
        super(code);
    }
}
