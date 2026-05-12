package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * 兑现批量广播 — 节点每秒累积本周期所有兑现(本地玩家+集群同步过来)，统一推送给本地玩家
 *
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID, cmd = AirRaidConstant.MsgBean.NOTIFY_AIR_RAID_CASH_OUT, resp = true)
@ProtoDesc("兑现批量广播")
public class NotifyAirRaidCashOut extends AbstractNotice {
    @ProtoDesc("兑现列表")
    public List<AirRaidCashOutInfo> playerCashOuts = new ArrayList<>();
}
