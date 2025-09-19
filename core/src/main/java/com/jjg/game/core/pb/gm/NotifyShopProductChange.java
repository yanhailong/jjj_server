package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/9/18 10:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_SHOP_PRODUCT_CHANGE, resp = true,toPbFile = false)
@ProtoDesc("通知商城商品变更")
public class NotifyShopProductChange extends AbstractNotice {
}
