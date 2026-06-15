package com.jjg.game.alliance.pb.res;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.alliance.pb.struct.AllianceShopGoodsInfo;

import java.util.List;

/**
 * 联盟商店列表返回。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ALLIANCE, cmd = AllianceConst.MsgBean.RES_SHOP_LIST, resp = true)
@ProtoDesc("联盟商店列表返回")
public class ResShopList extends AbstractResponse {
    @ProtoDesc("商品列表")
    public List<AllianceShopGoodsInfo> goods;
    @ProtoDesc("我的贡献值")
    public long myContribution;
    @ProtoDesc("商店刷新时间(ms, 次日0点)")
    public long refreshTime;

    public ResShopList(int code) {
        super(code);
    }
}
