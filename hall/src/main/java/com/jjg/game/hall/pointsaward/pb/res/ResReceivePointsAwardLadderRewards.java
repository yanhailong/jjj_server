package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求领取积分大奖积分累计的阶梯奖励回返
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_RECEIVE_POINTS_AWARD_LADDER_REWARD,
        resp = true
)
@ProtoDesc("请求领取积分大奖积分累计的阶梯奖励回返")
public class ResReceivePointsAwardLadderRewards extends AbstractResponse {

    /**
     * 领取的阶段的积分值
     */
    @ProtoDesc("请求领取的阶段的积分值")
    private long points;

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public ResReceivePointsAwardLadderRewards(int code) {
        super(code);
    }

}
