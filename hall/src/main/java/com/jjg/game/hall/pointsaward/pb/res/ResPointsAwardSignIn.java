package com.jjg.game.hall.pointsaward.pb.res;


import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求积分大奖签到回返
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_SIGN,
        resp = true
)
@ProtoDesc("请求积分大奖签到回返")
public class ResPointsAwardSignIn extends AbstractResponse {

    /**
     * 当前签到总天数
     */
    @ProtoDesc("当前签到总天数")
    private int count;

    public ResPointsAwardSignIn(int code) {
        super(code);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
