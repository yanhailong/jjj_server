package com.jjg.game.sim.season.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.season.pb.struct.SeasonShopItemInfo;
import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SEASON_SHOP, resp = true)
@ProtoDesc("赛季商店返回")
public class ResSeasonShop extends AbstractResponse {
    @ProtoDesc("商品列表")
    public List<SeasonShopItemInfo> items;
    @ProtoDesc("当前赛季币")
    public long seasonCoin;

    public ResSeasonShop(int code) {
        super(code);
    }
}
