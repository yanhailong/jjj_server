package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.NOTIFY_AIR_RAID_CRASH, resp = true)
@ProtoDesc("坠毁广播")
public class NotifyAirRaidCrash extends AbstractNotice {
    @ProtoDesc("坠毁时间")
    public int crashTime;
    @ProtoDesc("坠毁倍率(万分比，10000=1.00x)")
    public int crashMultiplier;
}
