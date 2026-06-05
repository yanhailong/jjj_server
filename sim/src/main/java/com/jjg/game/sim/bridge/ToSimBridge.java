package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sim.data.SimSkillsData;

import java.util.Map;

/**
 * @author 11
 * @date 2026/5/25
 */
public interface ToSimBridge extends IGameRpc {
    /**
     * 扣除研究点
     *
     * @return
     */
    int deductResearchPoint(long playerId, Map<Integer, Integer> deductMap);

    /**
     * 添加技能
     *
     * @param playerId
     * @param gameType
     * @param skillId
     * @return
     */
    CommonResult<SimSkillsData> addSkillById(long playerId, int gameType, int skillId);

    /**
     * slots 旋转联动: 扣能量 -> 加经验 -> 赌场升级 -> 道具掉落入背包
     *
     * @param playerId 玩家id (供 RPC 按玩家路由)
     * @param gameType slots 游戏类型 (如 SuperStar=100300)
     * @param winTimes 本次中奖倍数 (allWinGold / allBetScore)
     * @return code
     */
    int onSlotsSpin(long playerId, int gameType, int winTimes);
}
