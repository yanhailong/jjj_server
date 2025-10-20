package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/20 11:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_GENERATE_ORDER)
@ProtoDesc("预下单请求")
public class ReqGenerateOrder extends AbstractMessage {
    @ProtoDesc("支付方式  0.google   1.ios")
    public int payType;
    @ProtoDesc("来源")
    public RechargeType rechargeType;
    @ProtoDesc("商品id")
    public String productId;
}
