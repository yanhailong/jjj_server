package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.ShopConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/18 14:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = ShopConstant.MsgBean.RES_BUY_PRODUCT,resp = true)
@ProtoDesc("返回下单")
public class ResBuyProduct extends AbstractResponse {
    @ProtoDesc("订单id  如果是充值类，则返回订单id")
    public String orderId;
    @ProtoDesc("添加的道具信息，如果是道具兑换类，则返回添加成功的道具")
    public List<ItemInfo> items;

    public ResBuyProduct(int code) {
        super(code);
    }
}
