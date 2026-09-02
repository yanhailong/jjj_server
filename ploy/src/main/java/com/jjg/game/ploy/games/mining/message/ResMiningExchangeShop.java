package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.mining.MiningConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_EXCHANGE_SHOP, resp = true)
@ProtoDesc("挖矿兑换商店")
public class ResMiningExchangeShop extends AbstractResponse {
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("每日限购刷新时间毫秒")
    public long nextDailyReset;
    @ProtoDesc("商店所用货币余额")
    public List<ItemInfo> currencies;
    @ProtoDesc("兑换商品")
    public List<MiningExchangeInfo> goods;

    public ResMiningExchangeShop(int code) { super(code); }
}
