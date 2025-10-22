package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.ShopConstant;

/**
 * @author 11
 * @date 2025/9/17 16:09
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SHOP_TYPE, cmd = ShopConstant.MsgBean.REQ_SHOP)
@ProtoDesc("请求获取商城")
public class ReqShop extends AbstractMessage {
    @ProtoDesc("渠道id  1.google  2.apple")
    public int channel;
}
