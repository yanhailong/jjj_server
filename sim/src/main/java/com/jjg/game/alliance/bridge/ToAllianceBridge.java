package com.jjg.game.alliance.bridge;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * 其他节点 -> 联盟系统 的跨节点 RPC 接口。
 * <p>
 * 范式对齐 {@link com.jjg.game.sim.bridge.ToSimBridge}: 由游戏节点(slots/table/poker)通过
 * {@code @ClusterRpcReference} 调用; hall 侧由 {@code HallRPCController} 实现
 * (转发到 AllianceEventService / AllianceCacheService)。
 * <p>
 * 这是其他玩法接入联盟任务/对决的跨节点入口; hall 进程内的调用方直接注入
 * {@code AllianceEventService} 即可, 不必走本接口。
 *
 * @author 11
 * @date 2026/6/11
 */
public interface ToAllianceBridge extends IGameRpc {

    /**
     * 上报玩家赚取金币 (EARN_GOLD 任务进度): 由游戏节点在结算处调用。
     *
     * @param playerId 玩家id
     * @param gameType 玩法类型
     * @param gold     本次赚取金币数
     */
    void reportEarnGold(long playerId, int gameType, long gold);

    /**
     * 通用联盟事件上报 (任务进度): 新玩法接入时无需新增 RPC 方法。
     *
     * @param goalType 目标类型 (AllianceConst.TaskGoalType)
     * @param param    事件参数
     * @param value    增量
     */
    void reportAllianceEvent(long playerId, int goalType, long param, long value);

    /**
     * 查询玩家所在联盟 id (0=无盟): 供游戏节点做联盟相关展示/校验。
     */
    long getPlayerAllianceId(long playerId);
}
