package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimItemOperationResult;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.dao.SimTaskDao;
import com.jjg.game.sim.data.SimTaskData;
import com.jjg.game.sim.pb.res.NotifySimTaskUpdate;
import com.jjg.game.sim.pb.res.ResSimTaskList;
import com.jjg.game.sim.pb.res.ResSimTaskReward;
import com.jjg.game.sim.pb.res.ResSetDisplayedMedals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * sim 主线/成就任务服务 (线性链)。
 * <p>
 * 条件判定复用 core 通用条件系统 {@link ConditionManager}: 把任务配置 {@code taskConditionId}
 * (如 [10001,0,1,10]) 转成条件表达式 (如 {@code betFrequency(0,1,10)}, 首位经 condition 表映射为类型名),
 * 进度由条件系统累计在 Redis(CountDao)。prefix 按"主线整条 / 每个成就组"隔离, 同 prefix 同类型跨阶梯共享累计计数。
 * 链生命周期(接取/推进/领奖)与持久化由本服务在 sim 内自管, 事件来源 {@code SimManager.onSlotsSpin}。
 *
 * @author 11
 * @date 2026/6/25
 */
@Service
public class SimTaskService {
    private static final Logger log = LoggerFactory.getLogger(SimTaskService.class);

    //计数 prefix: 主线整条共享一个, 成就每组一个 (featureId = 条件type + prefix)
    private static final String PREFIX_MAIN = "simTaskMain";
    private static final String PREFIX_ACH = "simTaskAch";
    private static final int MAX_DISPLAYED_MEDALS = 3;

    @Autowired
    private SimTaskConfigService taskConfig;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private ConditionManager conditionManager;
    @Autowired
    private CountDao countDao;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SimMedalService simMedalService;
    @Autowired
    private TaskLogger taskLogger;

    // =====================================================================
    // 加载 / 接取
    // =====================================================================

    /**
     * 登录加载任务数据: 无则新建并接取主线首节点+各成就组首节点; 已有则补齐(新成就组/主线续接)。
     */
    public void initTaskData(SimPlayerContext ctx) {
        long playerId = ctx.playerId();
        SimTaskData data = simTaskDao.findById(playerId).orElse(null);
        if (data == null) {
            data = new SimTaskData();
            data.setPlayerId(playerId);
        }
        ctx.setSimTaskData(data);
        ensureActive(playerId, data);
    }

    /**
     * 由任务链当前节点补齐历史完成数量。配置缩减时不回退已经累计的数值。
     */
    public void reconcileFinishedTaskCount(SimPlayerContext ctx) {
        SimTaskData data = ctx.getSimTaskData();
        SimBaseData baseData = ctx.getSimBaseData();
        if (data == null || baseData == null) {
            return;
        }
        int completed = completedCount(data.getMainTask());
        for (TaskDetail node : data.getAchievements().values()) {
            completed += completedCount(node);
        }
        if (completed > baseData.getFinishedTaskCount()) {
            baseData.setFinishedTaskCount(completed);
        }
    }

    private int completedCount(TaskDetail node) {
        if (node == null) {
            return 0;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
        boolean currentCompleted = node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS;
        return taskConfig.completedCountThrough(cfg, currentCompleted);
    }

    /**
     * 保证主线与各成就组都有一个"当前节点": 首次接取首节点; 末节点已领取且配置新增了后置则续接。
     */
    private void ensureActive(long playerId, SimTaskData data) {
        if (data.getMainTask() == null) {
            int firstMain = taskConfig.firstMain();
            if (firstMain > 0) {
                data.setMainTask(createNode(playerId, firstMain));
            }
        } else {
            advanceIfRewardedTail(playerId, data, data.getMainTask(), true, 0);
        }
        for (int group : taskConfig.achievementGroupIds()) {
            TaskDetail node = data.getAchievements().get(group);
            if (node == null) {
                int first = taskConfig.firstOf(group);
                if (first > 0) {
                    data.getAchievements().put(group, createNode(playerId, first));
                }
            } else {
                advanceIfRewardedTail(playerId, data, node, false, group);
            }
        }
    }

    /**
     * 末节点已领取后又新增了后置任务时, 把链续接到新的后置节点。
     */
    private void advanceIfRewardedTail(long playerId, SimTaskData data, TaskDetail node, boolean main, int group) {
        TaskDetail cur = node;
        while (cur != null && cur.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED) {
            int nextId = taskConfig.next(cur.getConfigId());
            if (nextId <= 0) {
                return;
            }
            TaskDetail next = createNode(playerId, nextId);
            if (main) {
                data.setMainTask(next);
            } else {
                data.getAchievements().put(group, next);
            }
            cur = next;
        }
    }

    private TaskDetail createNode(long playerId, int taskId) {
        TaskDetail node = new TaskDetail();
        node.setConfigId(taskId);
        node.setPlayerId(playerId);
        node.setStatus(TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        node.setCreateTime(System.currentTimeMillis());
        return node;
    }

    // =====================================================================
    // 进度 (旋转事件驱动)
    // =====================================================================

    /**
     * slots 旋转联动: 推进主线与各成就组的当前节点。仅在节点完成/续接时推送通知, 纯进度静默(客户端开界面拉取)。
     */
    public void onSpin(SimPlayerContext ctx, int gameType, SpinStatInfo statInfo) {
        try {
            SimTaskData data = ctx.getSimTaskData();
            if (data == null) {
                return;
            }
            Player player = resolvePlayer(ctx);
            if (player == null) {
                return;
            }
            BetEvent event = buildBetEvent(gameType, statInfo);
            List<Task> changed = new ArrayList<>();

            evaluateOnEvent(player, data, ctx.getSimBaseData(), data.getMainTask(), event, changed);
            //成就组在事件推进中可能续接(替换同 group 节点), 用 keySet 快照遍历
            for (Integer group : new ArrayList<>(data.getAchievements().keySet())) {
                evaluateOnEvent(player, data, ctx.getSimBaseData(), data.getAchievements().get(group), event, changed);
            }

            if (!changed.isEmpty()) {
                NotifySimTaskUpdate notify = new NotifySimTaskUpdate(Code.SUCCESS);
                notify.tasks = changed;
                ctx.send(notify);
            }
        } catch (Exception e) {
            log.error("sim 任务旋转联动异常 playerId={},gameType={}", ctx.playerId(), gameType, e);
        }
    }

    /**
     * 事件推进单个节点: 累计进度(条件系统) + 判定完成。
     */
    private void evaluateOnEvent(Player player, SimTaskData data, SimBaseData baseData, TaskDetail node, BetEvent event, List<Task> changed) {
        if (node == null || node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
            return;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
        if (cfg == null) {
            return;
        }
        String expr = exprOf(cfg);
        if (expr == null) {
            log.debug("任务[{}]条件无法解析表达式, 跳过 playerId={}", node.getConfigId(), player.getId());
            return;
        }
        String prefix = prefixOf(cfg);
        MatchResult r = conditionManager.addProgressAndGetAchievements(player, event, prefix, expr).result();
        //状态型条件(如玩家等级)addProgress 返回 UNKNOWN, 需再做一次达成判定
        boolean done = r == MatchResult.MATCH
                || (r == MatchResult.UNKNOWN && conditionManager.isAchievement(player, prefix, expr));
        if (done) {
            onComplete(player, data, baseData, node, cfg, changed);
        }
    }

    /**
     * 状态轮询(无事件): 开界面/上线时结算已达标的节点(覆盖玩家等级提升、计数已够等场景)。
     */
    private void pollState(Player player, SimTaskData data, SimBaseData baseData, TaskDetail node, List<Task> changed) {
        if (node == null || node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
            return;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
        if (cfg == null) {
            return;
        }
        String expr = exprOf(cfg);
        if (expr != null && conditionManager.isAchievement(player, prefixOf(cfg), expr)) {
            onComplete(player, data, baseData, node, cfg, changed);
        }
    }

    // =====================================================================
    // 完成 / 领奖 / 推进
    // =====================================================================

    /**
     * 节点完成: 置完成态; 无奖励则直接领取并续接, 有奖励则等待客户端领取。
     */
    private void onComplete(Player player, SimTaskData data, SimBaseData baseData, TaskDetail node, TaskCfg cfg, List<Task> changed) {
        long now = System.currentTimeMillis();
        node.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
        node.setCompleteTime(now);
        if (baseData != null) {
            baseData.incFinishedTaskCount();
        }
        //后台任务日志: 完成 (主线/成就类型由后台按 taskType 区分)
        taskLogger.completeTask(player.getId(), node.getConfigId());
        log.info("玩家[{}]完成 sim 任务[{}]", player.getId(), node.getConfigId());

        boolean noReward = (cfg.getGetItem() == null || cfg.getGetItem().isEmpty())
                && cfg.getIntegralNum() <= TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD;
        if (noReward) {
            node.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
            node.setRewardTime(now);
            taskLogger.receiveTaskAward(player.getId(), node.getConfigId(), null,
                    cfg.getIntegralNum(), TaskConstant.TaskStatus.STATUS_REWARDED);
            changed.add(assemble(player, node, cfg));
            Task next = advance(player, data, node, cfg);
            if (next != null) {
                changed.add(next);
            }
        } else {
            taskLogger.receiveTaskAward(player.getId(), node.getConfigId(), null,
                    cfg.getIntegralNum(), TaskConstant.TaskStatus.STATUS_COMPLETED);
            changed.add(assemble(player, node, cfg));
        }
    }

    /**
     * 领取任务奖励: 校验完成态 -> 发奖 -> 已领取 -> 续接下一节点。
     */
    public ResSimTaskReward claimReward(SimPlayerContext ctx, int taskId) {
        ResSimTaskReward res = new ResSimTaskReward(Code.SUCCESS);
        SimTaskData data = ctx.getSimTaskData();
        Player player = resolvePlayer(ctx);
        if (data == null || player == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        long playerId = player.getId();
        TaskCfg cfg = GameDataManager.getTaskCfg(taskId);
        if (cfg == null) {
            res.code = Code.NOT_FOUND;
            log.warn("领取 sim 任务奖励失败,配置不存在 playerId={},taskId={}", playerId, taskId);
            return res;
        }
        TaskDetail node = findActiveNode(data, cfg, taskId);
        if (node == null || node.getConfigId() != taskId) {
            res.code = Code.NOT_FOUND;
            log.warn("领取 sim 任务奖励失败,非当前节点 playerId={},taskId={}", playerId, taskId);
            return res;
        }
        if (node.getStatus() != TaskConstant.TaskStatus.STATUS_COMPLETED) {
            res.code = Code.FAIL;
            log.warn("领取 sim 任务奖励失败,任务状态[{}] playerId={},taskId={}", node.getStatus(), playerId, taskId);
            return res;
        }
        //发奖 (主线/成就奖励均为玩家背包道具; type2/3 当前无积分奖励)
        List<Item> rewardItems = null;
        if (cfg.getGetItem() != null && !cfg.getGetItem().isEmpty()) {
            CommonResult<SimItemOperationResult> addResult = simPackService.addItems(
                    ctx, cfg.getGetItem(), AddType.TASKAWARD, "taskId=" + taskId, true);
            if (addResult == null || !addResult.success()) {
                res.code = addResult == null ? Code.EXCEPTION : addResult.code;
                log.error("领取 sim 任务奖励失败,发奖失败 playerId={},taskId={},result={}",
                        playerId, taskId, addResult);
                return res;
            }
            rewardItems = toItemList(cfg.getGetItem());
        }
        node.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
        node.setRewardTime(System.currentTimeMillis());
        //后台任务日志: 奖励领取 (含领取内容)
        taskLogger.receiveTaskAward(playerId, taskId, rewardItems, cfg.getIntegralNum(),
                TaskConstant.TaskStatus.STATUS_REWARDED);

        res.taskId = taskId;
        res.nextTask = advance(player, data, node, cfg);
        //领奖后强制下个 tick 尽快落库(走 autosave 规范路径), 收窄崩溃重复领取窗口
        ctx.setLastSaveTime(0);
        //成就任务末节点奖励含勋章道具, 领取后可能新激活勋章 -> 刷新全服勋章榜分值
        if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
            simMedalService.refreshRankScore(ctx);
            simMedalService.refreshMedalBonusCache(ctx);
        }
        log.info("玩家[{}]领取 sim 任务[{}]奖励成功", playerId, taskId);
        return res;
    }

    /**
     * 链推进: 末节点保持已领取态(主线"任务预告"/成就组完成); 否则新建并激活下一节点。
     *
     * @return 新激活的下一节点协议体, 无后置返回 null
     */
    private Task advance(Player player, SimTaskData data, TaskDetail node, TaskCfg cfg) {
        int nextId = taskConfig.next(node.getConfigId());
        if (nextId <= 0) {
            return null;
        }
        TaskDetail next = createNode(player.getId(), nextId);
        if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            data.setMainTask(next);
        } else {
            data.getAchievements().put(cfg.getGroup(), next);
        }
        TaskCfg nextCfg = GameDataManager.getTaskCfg(nextId);
        return nextCfg == null ? null : assemble(player, next, nextCfg);
    }

    /**
     * 定位某 taskId 在数据中的当前节点 (主线或对应成就组)。
     */
    private TaskDetail findActiveNode(SimTaskData data, TaskCfg cfg, int taskId) {
        if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            return data.getMainTask();
        }
        return data.getAchievements().get(cfg.getGroup());
    }

    // =====================================================================
    // 列表 / 组装
    // =====================================================================

    /**
     * 任务列表: 主线当前节点 + 各成就组当前节点 (静态信息客户端依配置自取)。
     */
    public ResSimTaskList buildTaskList(SimPlayerContext ctx) {
        ResSimTaskList res = new ResSimTaskList(Code.SUCCESS);
        SimTaskData data = ctx.getSimTaskData();
        Player player = resolvePlayer(ctx);
        if (data == null || player == null) {
            return res;
        }
        //开界面时顺带补齐(配置热更新增成就组/主线续接) + 状态轮询结算
        ensureActive(player.getId(), data);
        List<Task> ignore = new ArrayList<>();
        pollState(player, data, ctx.getSimBaseData(), data.getMainTask(), ignore);
        for (Integer group : new ArrayList<>(data.getAchievements().keySet())) {
            pollState(player, data, ctx.getSimBaseData(), data.getAchievements().get(group), ignore);
        }

        TaskDetail main = data.getMainTask();
        if (main != null) {
            TaskCfg mainCfg = GameDataManager.getTaskCfg(main.getConfigId());
            if (mainCfg != null) {
                res.mainTask = assemble(player, main, mainCfg);
                res.mainFinished = main.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED
                        && taskConfig.next(main.getConfigId()) <= 0;
            }
        }
        List<Task> achievements = new ArrayList<>(data.getAchievements().size());
        for (TaskDetail node : data.getAchievements().values()) {
            TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
            if (cfg != null) {
                achievements.add(assemble(player, node, cfg));
            }
        }
        res.achievementTasks = achievements;
        List<Integer> activatedMedals = simMedalService.getActivatedMedalIds(ctx);
        Set<Integer> activated = new HashSet<>(activatedMedals);
        List<Integer> displayed = new ArrayList<>();
        for (Integer medalId : data.getDisplayedMedalIds()) {
            if (displayed.size() >= MAX_DISPLAYED_MEDALS) {
                break;
            }
            if (activated.contains(medalId) && !displayed.contains(medalId)) {
                displayed.add(medalId);
            }
        }
        if (!displayed.equals(data.getDisplayedMedalIds())) {
            data.setDisplayedMedalIds(displayed);
            ctx.setLastSaveTime(0);
        }
        //选择界面要求已展示勋章优先，其余按配置顺序排列
        List<Integer> sortedActivated = new ArrayList<>(displayed);
        for (Integer medalId : activatedMedals) {
            if (!activated.contains(medalId) || sortedActivated.contains(medalId)) {
                continue;
            }
            sortedActivated.add(medalId);
        }
        res.activatedMedalIds = sortedActivated;
        res.displayedMedalIds = new ArrayList<>(displayed);
        return res;
    }

    /**
     * 设置经营信息中展示的成就勋章。空列表表示全部取消展示。
     */
    public ResSetDisplayedMedals setDisplayedMedals(SimPlayerContext ctx, List<Integer> medalIds) {
        ResSetDisplayedMedals res = new ResSetDisplayedMedals(Code.SUCCESS);
        SimTaskData data = ctx.getSimTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }
        Set<Integer> activated = new HashSet<>(simMedalService.getActivatedMedalIds(ctx));
        if (!isValidDisplayedMedals(medalIds, activated)) {
            res.code = Code.PARAM_ERROR;
            res.medalIds = new ArrayList<>(data.getDisplayedMedalIds());
            return res;
        }
        List<Integer> displayed = copyDisplayedMedals(medalIds);
        data.setDisplayedMedalIds(displayed);
        ctx.setLastSaveTime(0);
        res.medalIds = new ArrayList<>(displayed);
        log.info("玩家[{}]设置经营信息展示勋章 {}", ctx.playerId(), displayed);
        return res;
    }

    /**
     * 任务道具奖励 Map 转道具列表 (后台日志领取内容)。
     */
    private static List<Item> toItemList(java.util.Map<Integer, Long> items) {
        List<Item> list = new ArrayList<>(items.size());
        items.forEach((id, count) -> list.add(new Item(id, count)));
        return list;
    }

    static boolean isValidDisplayedMedals(List<Integer> medalIds, Set<Integer> activated) {
        if (medalIds == null || medalIds.isEmpty()) {
            return true;
        }
        if (medalIds.size() > MAX_DISPLAYED_MEDALS || activated == null) {
            return false;
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>(medalIds);
        return unique.size() == medalIds.size()
                && !unique.contains(null)
                && activated.containsAll(unique);
    }

    static List<Integer> copyDisplayedMedals(List<Integer> medalIds) {
        return medalIds == null ? new ArrayList<>() : new ArrayList<>(medalIds);
    }

    /**
     * 组装任务协议体: configId + 状态 + 单条件(当前进度/目标/是否完成)。进度从条件系统(Redis计数)或玩家状态回读。
     */
    private Task assemble(Player player, TaskDetail node, TaskCfg cfg) {
        Task task = new Task();
        task.setConfigId(cfg.getId());
        task.setStatus(node.getStatus());

        List<Long> cond = cfg.getTaskConditionId();
        TaskCondition c = new TaskCondition();
        c.setConfigId(cond.getFirst().intValue());
        //目标值 = 条件配置末位 (本期 1/10001/12007 均如此)
        c.setConfigParam(cond.getLast());
        c.setProgress(currentProgress(player, cfg));
        c.setFinish(node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        task.getConditions().add(c);
        return task;
    }

    /**
     * 当前进度: 状态型(玩家等级)取实时等级, 事件型从条件系统计数(featureId = 条件type + prefix)回读。
     */
    private long currentProgress(Player player, TaskCfg cfg) {
        int condId = cfg.getTaskConditionId().getFirst().intValue();
        if (condId == TaskConstant.ConditionType.PLAYER_LEVEL) {
            return player.getLevel();
        }
        ConditionCfg cc = GameDataManager.getConditionCfg(condId);
        if (cc == null || cc.getTriggerEventType() == null || cc.getTriggerEventType().isEmpty()) {
            return 0;
        }
        return countDao.getCount(cc.getTriggerEventType() + prefixOf(cfg), String.valueOf(player.getId())).longValue();
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /**
     * taskConditionId -> 条件表达式: 首位经 condition 表映射为类型名, 其余作参数。
     * 例 [10001,0,1,10] -> betFrequency(0,1,10); [1,5] -> playerLevel(5); [12007,10000] -> totalValidBets(10000)。
     */
    private String exprOf(TaskCfg cfg) {
        List<Long> cond = cfg.getTaskConditionId();
        if (cond == null || cond.isEmpty()) {
            return null;
        }
        ConditionCfg cc = GameDataManager.getConditionCfg(cond.getFirst().intValue());
        if (cc == null || cc.getTriggerEventType() == null || cc.getTriggerEventType().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(cc.getTriggerEventType()).append('(');
        for (int i = 1; i < cond.size(); i++) {
            if (i > 1) {
                sb.append(',');
            }
            sb.append(cond.get(i));
        }
        return sb.append(')').toString();
    }

    /**
     * 计数隔离 prefix: 主线整条共享, 成就每组一个。
     */
    private String prefixOf(TaskCfg cfg) {
        return cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE
                ? PREFIX_MAIN
                : PREFIX_ACH + cfg.getGroup();
    }

    /**
     * 旋转 -> 通用条件事件: slots 非房间游戏(roomType<10), betList 单笔用于次数类条件。
     */
    private BetEvent buildBetEvent(int gameType, SpinStatInfo statInfo) {
        long bet = statInfo != null ? statInfo.getBet() : 0;
        long win = statInfo != null ? statInfo.getWin() : 0;
        BetEvent event = new BetEvent();
        event.setGameId(gameType);
        event.setGameType(gameType);
        event.setRoomType(0);
        event.setBetAmount(bet);
        event.setWinAmount(win);
        event.setBetList(List.of((int) Math.min(Math.max(bet, 1L), Integer.MAX_VALUE)));
        return event;
    }

    private Player resolvePlayer(SimPlayerContext ctx) {
        PlayerController pc = ctx.getPlayerController();
        if (pc != null && pc.getPlayer() != null) {
            return pc.getPlayer();
        }
        return corePlayerService.get(ctx.playerId());
    }
}
