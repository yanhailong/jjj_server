package com.jjg.game.room.constant;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.protostuff.MessageType;

/**
 * 游戏阶段枚举
 *
 * @author 2CL
 */

@ProtobufMessage
@ProtoDesc("阶段信息")
public enum EGamePhase {
    @ProtoDesc("游戏开始")
    START_GAME("游戏开始"),
    @ProtoDesc("下注")
    BET("下注"),
    @ProtoDesc("出牌")
    PLAY_CART("出牌"),
    @ProtoDesc("解散房间")
    DISS_MISS("解散房间"),
    @ProtoDesc("等待开始")
    WAIT_READY("等待开始"),
    @ProtoDesc("游戏一个回合结束进行结算")
    GAME_ROUND_OVER_SETTLEMENT("游戏一个回合结束进行结算"),
    ;

    /**
     * 每个阶段逻辑名
     */
    private final String phaseName;

    EGamePhase(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }
}
