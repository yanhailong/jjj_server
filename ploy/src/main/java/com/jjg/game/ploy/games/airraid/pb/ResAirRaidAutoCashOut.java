package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_AUTO_CASH_OUT, resp = true)
@ProtoDesc("自动兑现响应")
public class ResAirRaidAutoCashOut extends AbstractResponse {

    public ResAirRaidAutoCashOut(int code) {
        super(code);
    }
}
