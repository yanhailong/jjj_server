package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/13 11:12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_GET_REGISTER_REWARDS,resp = true)
@ProtoDesc("请求领取注册奖励")
public class ResGetRegisterRewards extends AbstractResponse {
    public ResGetRegisterRewards(int code) {
        super(code);
    }
}
