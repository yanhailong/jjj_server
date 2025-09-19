package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.ShopConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/17 16:10
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = ShopConstant.MsgBean.RES_SHOP,resp = true)
@ProtoDesc("商城返回")
public class ResShop extends AbstractResponse {
    @ProtoDesc("商品列表")
    public List<ShopProductInfo> shopProductInfoList;

    public ResShop(int code) {
        super(code);
    }
}
