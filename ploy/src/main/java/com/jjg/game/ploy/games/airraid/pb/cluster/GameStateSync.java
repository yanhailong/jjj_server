package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.airraid.data.AirRaidConstant;

/**
 * 游戏状态同步 — 主节点在阶段切换时广播给所有从节点
 * <p>
 * 从节点收到后更新本地 gameRoom 状态，并推送给本节点上的玩家。
 * </p>
 *
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PLOY_AIR_RAID,
        cmd = AirRaidConstant.MsgBean.GAME_STATE_SYNC, resp = true, toPbFile = false)
public class GameStateSync extends AbstractMessage {
    //当前回合号
    public int roundId;
    //游戏阶段
    public int phase;
    //阶段开始时间(ms)
    public long phaseStartTime;
    //阶段到期时间戳(ms)，客户端和从节点都据此计算剩余时间
    public long stopTime;
    //当前倍率(万分比), 飞行阶段有值
    public int currentMultiplier;
    //坠毁倍率(万分比)
    public int crashMultiplier;
}
