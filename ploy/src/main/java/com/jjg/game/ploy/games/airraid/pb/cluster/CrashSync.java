package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

import java.util.List;

/**
 * 坠毁同步 — 飞机坠毁时主节点广播给所有从节点
 *
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID,
        cmd = AirRaidConstant.MsgBean.CRASH_SYNC, resp = true, toPbFile = false)
public class CrashSync extends AbstractMessage {
    //当前回合号
    public int roundId;
    //坠毁倍率(万分比)
    public int crashMultiplier;
    //历史坠毁倍率列表
    public List<Integer> roundHistory;
}
