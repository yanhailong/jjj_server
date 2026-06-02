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
}
