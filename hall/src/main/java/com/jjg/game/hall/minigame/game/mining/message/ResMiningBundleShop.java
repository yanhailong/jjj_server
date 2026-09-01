package com.jjg.game.hall.minigame.game.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.minigame.game.mining.MiningConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MINIGAME, cmd = MiningConstant.RES_BUNDLE_SHOP, resp = true)
@ProtoDesc("挖矿礼包")
public class ResMiningBundleShop extends AbstractResponse {
    @ProtoDesc("当前赛季ID")
    public String seasonId;
    @ProtoDesc("当前存档版本")
    public long version;
    @ProtoDesc("每日限购刷新时间毫秒")
    public long nextDailyReset;
    @ProtoDesc("礼包")
    public List<MiningBundleInfo> bundles;

    public ResMiningBundleShop(int code) { super(code); }
}
