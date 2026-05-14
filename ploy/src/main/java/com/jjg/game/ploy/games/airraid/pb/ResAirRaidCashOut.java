package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_CASH_OUT, resp = true)
@ProtoDesc("兑现响应")
public class ResAirRaidCashOut extends AbstractResponse {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("兑现倍率(万分比，10000=1.00x)")
    public int cashOutMultiplier;
    @ProtoDesc("赢取金额")
    public long winAmount;
    @ProtoDesc("注单索引")
    public int betIndex;
    @ProtoDesc("时间戳ms")
    public long timestamp;

    public ResAirRaidCashOut(int code) {
        super(code);
    }
}
