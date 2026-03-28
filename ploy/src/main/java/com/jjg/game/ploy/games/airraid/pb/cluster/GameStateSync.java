package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;
import com.jjg.game.ploy.games.airraid.pb.AirRaidBetInfo;

import java.util.List;

/**
 * 游戏状态同步 — 主节点周期性广播给所有从节点
 *
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID,
        cmd = AirRaidConstant.MsgBean.GAME_STATE_SYNC, resp = true, toPbFile = false)
public class GameStateSync {
    //游戏阶段
    public int phase;
    //剩余时间(ms)
    public long remainTime;
    //当前倍率(万分比, 10000=1.00x)
    public int currentMultiplier;
    //本局坠毁倍率(万分比)
    public int crashMultiplier;
    //当前所有下注信息
    public List<AirRaidBetInfo> betInfoList;
    //历史坠毁倍率列表
    public List<Integer> roundHistory;
}
