package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_GUEST_POOL,resp = true)
@ProtoDesc("获取游客卡池返回")
public class ResGuestPool extends AbstractResponse {
    public List<Integer> guestIds;
    public ResGuestPool(int code) {
        super(code);
    }
}
