package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.ShopConstant;

/**
 * @author 11
 * @date 2025/9/18 14:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = ShopConstant.MsgBean.REQ_BUY_PRODUCT,resp = true)
@ProtoDesc("商城中物品兑换")
public class ReqBuyProduct extends AbstractMessage {
    @ProtoDesc("商品id")
    public int productId;
    @ProtoDesc("兑换个数")
    public int count;
}
