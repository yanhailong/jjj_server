package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 获取积分大奖积分累计的阶梯奖励
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_POINTS_AWARD_LADDER_REWARD
)
@ProtoDesc("获取积分大奖积分累计的阶梯奖励")
public class ReqPointsAwardLadderRewards extends AbstractMessage {
}
