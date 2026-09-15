package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.RankRewardInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_RANK_REWARDS, resp = true)
@ProtoDesc("返回人气排行榜奖励列表")
public class ResVisitRankRewards extends AbstractResponse {
    @ProtoDesc("按起始排名升序排列的奖励区间")
    public List<RankRewardInfo> rewards;

    public ResVisitRankRewards(int code) {
        super(code);
    }
}
