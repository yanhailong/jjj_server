package com.jjg.game.room.base;

import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;

/**
 * 游戏中循环的逻辑接口,比如百家乐,在游戏中的循环逻辑,1.洗牌 2.下注 3.庄家发牌，开牌 4.结算 => 1. 往复循环
 * 游戏中的阶段操作统一继承此接口
 *
 * @author 2CL
 */
public interface IRoomPhase {

    /**
     * 房间每个阶段的开始
     */
    void phaseDoAction();

    /**
     * 房间每个阶段的玩家行为，多为机器人行为或者托管行为
     */
    void playerPhaseAction();

    /**
     * 当玩家中途加入时调用
     */
    void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer);

    /**
     * 当玩家在阶段中途离开时触发
     */
    void onPlayerHalfwayExitPhase(GamePlayer gamePlayer);

    /**
     * 游戏阶段结束
     */
    void phaseFinish();

    /**
     * 每个阶段运行的时间，从配置读取或者立即执行(<=0时立即执行)
     */
    int getPhaseRunTime();

    /**
     * 通过判断是否跳到其他游戏阶段
     */
    default IRoomPhase bindNextPhase() {
        return null;
    }

    EGamePhase getGamePhase();

    /**
     * 回合计数器
     */
    void setRoundCounter(int roundCounter);

    /**
     * 回合计数器
     */
    int getRoundCounter();
}
