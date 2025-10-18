package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLadderRewardsInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取积分大奖积分累计的阶梯奖励回返
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_POINTS_AWARD_LADDER_REWARD,
        resp = true
)
@ProtoDesc("获取积分大奖积分累计的阶梯奖励回返")
public class ResPointsAwardLadderRewards extends AbstractResponse {

    /**
     * 当前总积分
     */
    @ProtoDesc("当前总积分")
    private long totalPoints;

    /**
     * 积分累计的阶段奖励配置
     */
    @ProtoDesc("积分累计的阶段奖励配置")
    private List<PointsAwardLadderRewardsInfo> ladderRewardsList = new ArrayList<>();

    public ResPointsAwardLadderRewards(int code) {
        super(code);
    }

    public List<PointsAwardLadderRewardsInfo> getLadderRewardsList() {
        return ladderRewardsList;
    }

    public void setLadderRewardsList(List<PointsAwardLadderRewardsInfo> ladderRewardsList) {
        this.ladderRewardsList = ladderRewardsList;
    }

    public long getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(long totalPoints) {
        this.totalPoints = totalPoints;
    }
}
