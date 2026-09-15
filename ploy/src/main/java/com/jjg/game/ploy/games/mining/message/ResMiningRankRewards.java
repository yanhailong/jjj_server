package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.RankRewardInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.mining.MiningConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_RANK_REWARDS, resp = true)
@ProtoDesc("返回当前挖矿赛季排行榜奖励列表")
public class ResMiningRankRewards extends AbstractResponse {
    @ProtoDesc("按起始排名升序排列的奖励区间")
    public List<RankRewardInfo> rewards;

    public ResMiningRankRewards(int code) {
        super(code);
    }
}
