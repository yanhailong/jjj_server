package com.jjg.game.sim.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.pb.res.ResSeasonMatch;
import com.jjg.game.season.pb.res.ResSeasonTrialProgress;
import com.jjg.game.sim.data.SlotsEntrySessionData;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.data.FinishGuideRpcResult;
import com.jjg.game.sim.data.SkipGuideGroupRpcResult;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;
import com.jjg.game.sim.pb.res.ResSimTaskReward;
import com.jjg.game.sim.pb.res.ResVisitCasino;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2026/5/25
 */
public interface ToSimBridge extends IGameRpc {
    /** 在玩家所属 SIM 节点完成一个新手引导步骤。 */
    FinishGuideRpcResult finishGuide(long playerId, int guideId);

    /** 在玩家所属 SIM 节点跳过整个新手引导组。 */
    SkipGuideGroupRpcResult skipGuideGroup(long playerId, int guideGroupId);

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
     * 获取 slots 进场快照。普通入口包含 ResearchSkills + VisitorBonds，
     * 赛季入口包含 SeasonGem + VisitorBonds。
     *
     * @param skillOwnerId 技能归属玩家（客座赌局为房主）
     * @param casinoId   羁绊所属场景；0 表示玩家当前场景
     * @param gameType   游戏类型
     * @param seasonEntry 是否从赛季入口进入
     */
    CommonResult<SlotsEntrySessionData> getSlotsSessionData(long skillOwnerId, int casinoId,
                                                             int gameType, boolean seasonEntry);

    /**
     * slots spin
     *
     * @param playerId
     * @param gameType
     * @param winTimes
     * @param changeNode
     * @param statInfo   本次旋转统计明细 (用于经营信息 SPINE游戏面板; 可为 null)
     * @return 掉落结算及本次 sim 任务进度/状态变化，由 slots 节点统一通知客户端
     */
    CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                              SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit, int enterType);

    /**
     * 在玩家的 sim owner 节点领取主线/成就任务奖励。
     */
    ResSimTaskReward claimSimTaskReward(long playerId, int taskId);

    /** 在玩家所属 SIM 节点执行指定赌场拜访。 */
    ResVisitCasino visitCasino(long playerId, long targetPlayerId, int casinoId);

    /** 斗仙牌完成一次大结算后，批量推进真人玩家当前已接取的对应任务。 */
    CommonResult<Boolean> onDouXianSettled(List<Long> playerIds);

    /** 批量上报斗仙牌单回合中实际净赢为正的真人玩家。 */
    CommonResult<Boolean> onDouXianWins(int transactionItemId, Map<Long, Long> playerWins);

    /**
     * 发起赛季匹配，始终在玩家的 sim owner 节点执行。
     */
    ResSeasonMatch seasonMatch(long playerId, int gameType, long stake);

    /**
     * 发起一次不消耗主动匹配道具的被动赛季匹配。
     */
    ResSeasonMatch passiveSeasonMatch(long playerId, int gameType, long stake, long excludedSpinId);

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
    CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType, boolean freeMode);

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
     * 跨节点入账 sim 特殊资源 (能量/知名度/曝光度/赛季币/勋章)。
     * <p>
     * 这些资源的权威态在玩家 sim 会话所在节点的内存里，slots 等非 owner 节点取不到 ctx，
     * 必须转发到本接口执行；本接口内部不再二次路由。
     *
     * @param items 均为 sim 特殊资源, 常规道具由调用方自己的背包流程处理
     */
    CommonResult<Boolean> addSimItems(long playerId, List<Item> items, AddType addType, String desc);

    /**
     * 跨节点扣除 sim 特殊资源，语义同 {@link #addSimItems}，不足则整体失败且不产生扣除。
     */
    CommonResult<Boolean> removeSimItems(long playerId, List<Item> items, AddType addType, String desc);

    /**
     * 跨节点读取 sim 特殊资源持有量。直接读库会拿到最多落后一个落库周期的旧值，
     * 扣除前的余量校验必须以会话所在节点的内存态为准。
     */
    CommonResult<Long> getSimItemCount(long playerId, int itemId);

    /**
     * 背包整笔入账成功后的 sim 引导事件。
     * <p>
     * 由 slots 等非 owner 节点在本地监听器中转发；Hall 落点只操作本节点已有的 ctx。
     */
    CommonResult<Boolean> onPackItemsAdded(long playerId, Map<Integer, Long> items, AddType addType);

    /**
     * 背包整笔扣除成功后的 sim 任务事件。
     */
    CommonResult<Boolean> onPackItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType);

    /**
     * 非 SIM 节点向玩家所属 SIM 节点上报客户端新手引导事件。
     * 返回本次首次触发的引导组；重复上报成功但返回空列表。
     */
    CommonResult<List<Integer>> triggerGuideEvent(long playerId, int condition, int param);

    /**
     * 非 SIM 节点通知玩家已经进入指定引导场景。
     * 返回本次从场景等待状态激活的引导组。
     */
    CommonResult<List<Integer>> enterGuidePath(long playerId, String pathName);

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
