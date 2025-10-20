package com.jjg.game.hall.pointsaward.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;

/**
 * 请求转盘充值相关信息回复
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_TURNTABLE_RECHARGE_INFO,
        resp = true
)
@ProtoDesc("请求转盘充值相关信息回复")
public class ResTurntableRechargeInfo extends AbstractResponse {

    /**
     * 总充值金额
     */
    @ProtoDesc("总充值金额")
    private long rechargeValue;

    /**
     * 配置的金额
     */
    @ProtoDesc("配置的金额,每充值达到这个值就增加一次转盘次数")
    private int configValue;

    /**
     * 增加的转盘次数
     */
    @ProtoDesc("增加的转盘次数")
    private int addCount;

    public ResTurntableRechargeInfo(int code) {
        super(code);
    }

    public long getRechargeValue() {
        return rechargeValue;
    }

    public void setRechargeValue(long rechargeValue) {
        this.rechargeValue = rechargeValue;
    }

    public int getAddCount() {
        return addCount;
    }

    public void setAddCount(int addCount) {
        this.addCount = addCount;
    }

    public int getConfigValue() {
        return configValue;
    }

    public void setConfigValue(int configValue) {
        this.configValue = configValue;
    }
}
