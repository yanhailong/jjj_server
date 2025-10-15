package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求获取积分大奖的积分
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_POINT
)
@ProtoDesc("请求获取积分大奖的积分")
public class ReqPlayerPoint extends AbstractMessage {
}
