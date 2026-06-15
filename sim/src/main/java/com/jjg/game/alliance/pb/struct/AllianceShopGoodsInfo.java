package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 联盟商店商品项。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("联盟商店商品")
public class AllianceShopGoodsInfo {
    @ProtoDesc("商品id")
    public int goodsId;
    @ProtoDesc("道具id")
    public int itemId;
    @ProtoDesc("单次兑换数量")
    public long count;
    @ProtoDesc("价格(贡献值)")
    public long price;
    @ProtoDesc("每日限购次数")
    public int dailyLimit;
    @ProtoDesc("今日已购次数")
    public int boughtToday;
    @ProtoDesc("解锁所需联盟等级")
    public int unlockLevel;
    @ProtoDesc("当前是否已解锁")
    public boolean unlocked;
}
