package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.NOTIFY_AIR_RAID_BET, resp = true)
@ProtoDesc("通知下注信息")
public class NotifyAirRaidBet extends AbstractNotice {
    @ProtoDesc("下注信息")
    public List<AirRaidBetInfo> betInfoList;
}
