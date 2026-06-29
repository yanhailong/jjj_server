package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SET_DISPLAYED_MEDALS, resp = true)
@ProtoDesc("设置经营信息展示的成就勋章返回")
public class ResSetDisplayedMedals extends AbstractResponse {
    @ProtoDesc("设置成功后的展示勋章配置id")
    public List<Integer> medalIds;

    public ResSetDisplayedMedals(int code) {
        super(code);
    }
}
