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
 * 请求积分大奖签到配置回返
 */
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.HALL_TYPE,
        cmd = PointsAwardConstant.Message.RES_SIGN_CONFIG,
        resp = true
)
@ProtoDesc("请求积分大奖签到配置回返")
public class ResPointsAwardSignInConfig extends AbstractResponse {

    /**
     * 当前签到总天数
     */
    @ProtoDesc("当前签到总天数")
    private int count;

    /**
     * 配置数据
     */
    @ProtoDesc("配置数据,条数等于本月天数")
    private List<PointsAwardSignInConfig> configList = new ArrayList<>();

    public ResPointsAwardSignInConfig(int code) {
        super(code);
    }

    public List<PointsAwardSignInConfig> getConfigList() {
        return configList;
    }

    public void setConfigList(List<PointsAwardSignInConfig> configList) {
        this.configList = configList;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
