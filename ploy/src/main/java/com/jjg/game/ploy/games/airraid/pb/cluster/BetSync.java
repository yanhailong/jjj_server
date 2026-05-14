package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;
import com.jjg.game.ploy.games.airraid.pb.AirRaidPlayerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 下注同步 — 有玩家下注时主节点广播给所有从节点
 *
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID,
        cmd = AirRaidConstant.MsgBean.BET_SYNC, resp = true, toPbFile = false)
public class BetSync extends AbstractMessage {
    //当前回合号
    public int roundId;
    //玩家下注信息
    public List<AirRaidPlayerInfo> playerBetInfoList = new ArrayList<>();
}
