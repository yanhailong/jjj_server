package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_BET, resp = true)
@ProtoDesc("下注返回")
public class ResAirRaidBet extends AbstractResponse {
    @ProtoDesc("当前余额")
    public long gold;

    public ResAirRaidBet(int code) {
        super(code);
    }
}
