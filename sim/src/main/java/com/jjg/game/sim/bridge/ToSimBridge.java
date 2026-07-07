package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;

import java.util.List;
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

    /**
     * 多人协作任务结算回写 (slots 房间结束时调用发起者所在 sim 节点):
     * 发起者任务态 -> 待领奖/失败; 协助者奖励经邮件发放。
     *
     * @param ownerId   发起者
     * @param taskId    任务配置id
     * @param roomId    结算所属房间id
     * @param success   任务是否完成
     * @param helperIds 协助者 (不含发起者)
     */
    CommonResult<Boolean> onCoopRoomSettle(long ownerId, int taskId, long roomId, boolean success, List<Long> helperIds);
}
