package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2026/5/13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_RANK, resp = true)
@ProtoDesc("排行榜响应")
public class ResAirRaidRank extends AbstractResponse {
    @ProtoDesc("排行列表(已按排序维度倒序)")
    public List<AirRaidRankInfo> rankList;
    @ProtoDesc("排序维度: 0=中奖倍数 1=中奖金额  2=回合")
    public int rankType;
    @ProtoDesc("时间周期: 0=日 1=月 2=年")
    public int period;

    public ResAirRaidRank(int code) {
        super(code);
    }
}
