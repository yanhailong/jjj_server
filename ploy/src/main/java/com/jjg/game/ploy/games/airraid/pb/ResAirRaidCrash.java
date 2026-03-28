package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_CRASH, resp = true)
@ProtoDesc("坠毁广播")
public class ResAirRaidCrash extends AbstractResponse {
    @ProtoDesc("坠毁倍率(万分比，10000=1.00x)")
    public int crashMultiplier;
    @ProtoDesc("回合历史")
    public List<Integer> roundHistory;

    public ResAirRaidCrash(int code) {
        super(code);
    }
}
