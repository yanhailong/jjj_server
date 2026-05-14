package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/5/13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.REQ_AIR_RAID_RANK)
@ProtoDesc("排行榜请求")
public class ReqAirRaidRank extends AbstractMessage {
    @ProtoDesc("排序维度: 0=中奖倍数 1=中奖金额  2=回合")
    public int rankType;
    @ProtoDesc("时间周期: 0=日 1=月 2=年")
    public int period;
}
