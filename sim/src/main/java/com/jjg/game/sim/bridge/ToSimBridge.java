package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.season.pb.res.ResSeasonMatch;
import com.jjg.game.season.pb.res.ResSeasonTrialProgress;
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
     * 添加技能
     *
     * @param playerId
     * @param gameType
     * @param skillId
     * @return
     */
    CommonResult<SimSkillsData> addSkillById(long playerId, int gameType, int skillId);

    /**
     * 获取玩家指定游戏的最新技能数据 (在线以 sim 内存态为准, 离线回退读库)。
     * 供 slots 进游戏时读取, 避免 sim 内存改动未到定时落库导致的脏读。
     *
     * @param playerId 技能归属玩家 (客座赌局为房主)
     * @param gameType 游戏类型
     * @return data 可能为 null (该游戏无技能数据)
     */
    CommonResult<SimSkillsData> getSkillData(long playerId, int gameType);

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
     * 发起赛季匹配，始终在玩家的 sim owner 节点执行。
     */
    ResSeasonMatch seasonMatch(long playerId, int gameType, long stake);

    /**
     * 获取进行中的赛季试炼进度，始终在玩家的 sim owner 节点执行。
     */
    ResSeasonTrialProgress seasonTrialProgress(long playerId);

    /**
     * 在玩家的 sim owner 节点调整个人赛季测试时间。
     */
    CommonResult<String> seasonGm(long playerId, String[] orders);

    /**
     * 赛季每日免费局: 扣费前申请消耗一次免费次数 (仅赛季机台默认下注的普通旋转会调用)。
     */
    CommonResult<SeasonFreeSpinResult> useSeasonFreeSpin(long playerId, int gameType);

    /**
     * 获取赛季 slots 进场快照。快照包含当前赛季币余额，以及已镶嵌宝石中匹配当前游戏的效果。
     */
    CommonResult<SeasonSlotsSessionData> getSeasonSlotsSessionData(long playerId, int gameType);

    /**
     * 旧 slots 节点滚动升级期间使用的余额查询兼容入口；新节点统一使用进场快照接口。
     */
    @Deprecated
    CommonResult<Long> getSeasonCoin(long playerId);

    /**
     * 从赛季进入的 slots 下注: 扣除玩家赛季币。按 transactionId 幂等 (超时重试同一 id 不重复扣)。
     * 余额不足返回 {@link com.jjg.game.core.constant.Code#NOT_ENOUGH}; 成功时 data 为扣除后的最新余额。
     */
    CommonResult<Long> deductSeasonCoin(long playerId, long amount, long transactionId);

    /**
     * 从赛季进入的 slots 中奖: 给玩家增加赛季币 (仅加余额, 不计入段位累计)。
     * 按 transactionId 幂等 (超时重试同一 id 不重复发); 成功时 data 为增加后的最新余额。
     */
    CommonResult<Long> addSeasonCoin(long playerId, long amount, long transactionId);

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
     * 获取玩家战力 (遍历所有游戏技能, 累加 ResearchSkillsCfg 的战力值; 玩家不在线返回 0)。
     *
     * @param playerId
     * @return
     */
    int getCombatPower(long playerId);

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
