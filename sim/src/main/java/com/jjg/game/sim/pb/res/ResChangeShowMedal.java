package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/1
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_CHANGE_SHOW_MEDAL, resp = true)
@ProtoDesc("修改展示的勋章返回")
public class ResChangeShowMedal extends AbstractResponse {
    @ProtoDesc("展示中的勋章")
    public List<Integer> showMedalIds;

    public ResChangeShowMedal(int code) {
        super(code);
    }
}
