package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.GuestInfo;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_GEN_PURCHASED_GUEST, resp = true)
@ProtoDesc("生成购买游客返回")
public class ResGenPurchasedGuest extends AbstractResponse {
    @ProtoDesc("购买游客信息 (uid + 目的地, 奖励需另发领奖请求)")
    public GuestInfo guest;

    public ResGenPurchasedGuest(int code) {
        super(code);
    }
}
