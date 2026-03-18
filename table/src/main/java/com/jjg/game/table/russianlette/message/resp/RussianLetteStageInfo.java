package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘基础信息")
public class RussianLetteStageInfo {

    /** 当前游戏阶段：REST / BET / DRAW_ON / GAME_ROUND_OVER_SETTLEMENT */
    @ProtoDesc("当前阶段")
    public EGamePhase gamePhase;

    /** 本阶段结束时间戳（毫秒），客户端据此计算剩余倒计时 */
    @ProtoDesc("结束时间戳(ms)")
    public long endTime;

    /** 开奖结果 1-37（37 代表绿色 0） */
    @ProtoDesc("开奖结果")
    public int diceData;
}
