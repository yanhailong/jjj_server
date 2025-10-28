package com.jjg.game.hall.pointsaward.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求积分大奖的签到
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.REQ_SIGN
)
@ProtoDesc("请求积分大奖的签到")
public class ReqPointsAwardSignIn extends AbstractMessage {

    /**
     * 领取奖励的签到天数对应协议 签到配置的dayOfMonth字段
     */
    @ProtoDesc("领取奖励的签到天数对应协议 签到配置的dayOfMonth字段")
    private int dayOfMonth;

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }
}
