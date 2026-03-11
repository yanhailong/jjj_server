package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 房间摘要，进入场次时展示
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘房间摘要")
public class RussianLetteSummary {

    @ProtoDesc("roomId")
    public long roomId;

    /** 当前游戏阶段：REST / BET / DRAW_ON / GAME_ROUND_OVER_SETTLEMENT */
    @ProtoDesc("当前阶段")
    public RussianLetteStageInfo stageInfo;

    @ProtoDesc("俄罗斯转盘游戏基础信息")
    public RussianLetteBaseInfo baseInfo;

    @ProtoDesc("转盘结果 只记录最新12把的数字（0-36）")
    public List<Integer> cardStateList;

    @ProtoDesc("概率信息")
    public RussianLetteProb prob;

    @ProtoDesc("房间场次说明")
    public int roomType;

}
