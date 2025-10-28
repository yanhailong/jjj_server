package com.jjg.game.hall.pointsaward.pb.res;


import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardSignInConfig;

import java.util.ArrayList;
import java.util.List;

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
     * 领取奖励的签到天数对应协议 签到配置的dayOfMonth字段
     */
    @ProtoDesc("领取奖励的签到天数对应协议 签到配置的dayOfMonth字段")
    private int dayOfMonth;

    /**
     * 配置数据
     */
    @ProtoDesc("签到后的配置数据,条数等于本月天数,签到成功失败都会返回整个列表")
    private List<PointsAwardSignInConfig> configList = new ArrayList<>();

    public ResPointsAwardSignIn(int code) {
        super(code);
    }

    public List<PointsAwardSignInConfig> getConfigList() {
        return configList;
    }

    public void setConfigList(List<PointsAwardSignInConfig> configList) {
        this.configList = configList;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }
}
