package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.NOTIFY_AIR_RAID_GAME_STATE, resp = true)
@ProtoDesc("游戏状态广播")
public class NotifyAirRaidGameState extends AbstractNotice {
    @ProtoDesc("游戏阶段")
    public int phase;
    @ProtoDesc("阶段结束时间(毫秒)")
    public long stopTime;
    @ProtoDesc("坠毁倍率(万分比)")
    public int crashMultiplier;
}
