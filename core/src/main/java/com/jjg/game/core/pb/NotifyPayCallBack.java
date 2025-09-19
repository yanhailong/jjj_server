package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.ShopConstant;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/18 16:32
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = ShopConstant.MsgBean.NOTIFY_PAY_CALLBACK,resp = true)
@ProtoDesc("返回下单")
public class NotifyPayCallBack {
    @ProtoDesc("订单id")
    public String orderId;
    @ProtoDesc("获得道具")
    public List<ItemInfo> items;
}
