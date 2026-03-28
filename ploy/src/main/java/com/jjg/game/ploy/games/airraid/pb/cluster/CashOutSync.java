package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * 兑现同步 — 有玩家兑现时主节点广播给所有从节点
 *
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID,
        cmd = AirRaidConstant.MsgBean.CASH_OUT_SYNC, resp = true, toPbFile = false)
public class CashOutSync {
    //玩家ID
    public long playerId;
    //兑现倍率(万分比)
    public int cashOutMultiplier;
    //赢得金额
    public long winAmount;
    //注单索引(0或1)
    public int betIndex;
}
