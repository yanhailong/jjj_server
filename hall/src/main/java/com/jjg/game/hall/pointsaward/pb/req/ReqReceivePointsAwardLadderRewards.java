package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_RECEIVE_POINTS_AWARD_LADDER_REWARD
)
@ProtoDesc("请求领取积分大奖积分累计的阶梯奖励")
public class ReqReceivePointsAwardLadderRewards extends AbstractMessage {

    /**
     * 请求领取的阶段的积分值
     */
    @ProtoDesc("请求领取的阶段的积分值")
    private long points;

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }
}
