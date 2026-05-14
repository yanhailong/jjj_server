package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.REQ_AIR_RAID_CASH_OUT)
@ProtoDesc("兑现请求")
public class ReqAirRaidCashOut extends AbstractMessage {
    @ProtoDesc("注单索引 0或1")
    public int betIndex;
    @ProtoDesc("飞行持续时间")
    public long flyTime;
}
