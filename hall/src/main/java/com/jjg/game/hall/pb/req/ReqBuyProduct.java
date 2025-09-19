package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.ShopConstant;

/**
 * @author 11
 * @date 2025/9/18 14:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = ShopConstant.MsgBean.REQ_BUY_PRODUCT,resp = true)
@ProtoDesc("返回下单")
public class ReqBuyProduct extends AbstractMessage {
    @ProtoDesc("商品id")
    public int productId;
}
