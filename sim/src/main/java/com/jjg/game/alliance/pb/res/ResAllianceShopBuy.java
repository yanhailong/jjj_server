package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 购买商品返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_SHOP_BUY, resp = true)
@ProtoDesc("购买商品返回")
public class ResAllianceShopBuy extends AbstractResponse {
    @ProtoDesc("商品id")
    public int goodsId;
    @ProtoDesc("道具id")
    public int itemId;
    @ProtoDesc("数量")
    public long count;
    @ProtoDesc("购买后的贡献值")
    public long myContribution;

    public ResAllianceShopBuy(int code) {
        super(code);
    }
}
