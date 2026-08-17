package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.SpecialGuestInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_BUY_SPECIAL_GUEST, resp = true)
@ProtoDesc("购买特殊游客返回")
public class ResBuySpecialGuest extends AbstractResponse {
    @ProtoDesc("现金订单id，广告和钻石购买时为空")
    public String orderId;
    @ProtoDesc("特殊游客最新信息；广告购买成功时为替换后的展示配置")
    public SpecialGuestInfo specialGuest;

    public ResBuySpecialGuest(int code) {
        super(code);
    }
}
