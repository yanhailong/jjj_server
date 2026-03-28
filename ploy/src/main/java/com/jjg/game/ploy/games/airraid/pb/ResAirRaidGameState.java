package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.RES_AIR_RAID_GAME_STATE, resp = true)
@ProtoDesc("游戏状态广播")
public class ResAirRaidGameState extends AbstractResponse {
    @ProtoDesc("游戏阶段")
    public int phase;
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("当前倍率(万分比，10000=1.00x)")
    public int currentMultiplier;

    public ResAirRaidGameState(int code) {
        super(code);
    }
}
