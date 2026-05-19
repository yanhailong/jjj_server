package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_LAST_ROUND, resp = true)
@ProtoDesc("获取上一回合的信息返回")
public class ResAirRaidLastRound extends AbstractResponse {
    @ProtoDesc("坠毁倍率(万分比)")
    public int crashMultiplier;
    @ProtoDesc("回合中的玩家信息")
    public List<AirRaidPlayerInfo> roundPlayerInfoList;

    public ResAirRaidLastRound(int code) {
        super(code);
    }
}
