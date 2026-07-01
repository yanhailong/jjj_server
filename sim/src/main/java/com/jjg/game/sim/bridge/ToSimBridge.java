package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;

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
    CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                             SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit);

    /**
     * 客座赌局每次旋转前授权；普通旋转由 slots 本地直接跳过该 RPC。
     */
    CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType);

    /**
     * slots 生成结果失败时退回试玩次数和能量。
     */
    CommonResult<Boolean> cancelVisitTrialSpin(long playerId, VisitTrialSpinPermit permit);

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
