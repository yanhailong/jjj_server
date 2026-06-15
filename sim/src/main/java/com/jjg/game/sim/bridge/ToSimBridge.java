package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;

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
     * slots spin
     *
     * @param playerId
     * @param gameType
     * @param winTimes
     * @param changeNode
     * @param statInfo   本次旋转统计明细 (用于经营信息 SPINE游戏面板; 可为 null)
     * @return
     */
    CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode, SpinStatInfo statInfo);

    /**
     * 升级技能
     *
     * @param playerId
     * @param gameType
     * @param skillId
     * @return
     */
    CommonResult<Map<Integer, Integer>> skillLevelUp(long playerId, int gameType, int skillId);
}
