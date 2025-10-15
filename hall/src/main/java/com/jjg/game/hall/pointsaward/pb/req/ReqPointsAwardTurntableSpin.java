package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求转盘旋转
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_TURNTABLE
)
@ProtoDesc("请求转盘旋转")
public class ReqPointsAwardTurntableSpin extends AbstractMessage {
}
