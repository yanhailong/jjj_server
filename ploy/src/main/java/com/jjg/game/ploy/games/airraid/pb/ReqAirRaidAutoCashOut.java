package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.REQ_AIR_RAID_AUTO_CASH_OUT)
@ProtoDesc("自动兑现请求")
public class ReqAirRaidAutoCashOut extends AbsNodeMessage {
    @ProtoDesc("注单索引 0或1")
    public int betIndex;
    @ProtoDesc("是否开启")
    public boolean open;
    @ProtoDesc("坠毁倍率(万分比)")
    public int crashMultiplier;
}
