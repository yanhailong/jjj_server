package com.jjg.game.sim.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.*;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.core.task.condition.TaskCondition12001;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.TaskConditionParam12001;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.dao.SimTaskDao;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimTaskData;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.listener.SimTaskStateReporter;
import com.jjg.game.sim.logger.SimAchievementTaskLogger;
import com.jjg.game.sim.logger.SimMainTaskLogger;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.res.NotifySimTaskUpdate;
import com.jjg.game.sim.pb.res.ResSetDisplayedMedals;
import com.jjg.game.sim.pb.res.ResSimTaskList;
import com.jjg.game.sim.pb.res.ResSimTaskReward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * sim 主线/成就任务服务 (线性链)。
 * <p>
 * 条件判定复用 core 数值条件规则：配置加载时完成解析校验，事件热路径只做 O(1) 规则求值。
 * 普通条件进度由本功能累计在 Redis(CountDao)；玩家统计条件直接读取玩家级统计。prefix 按"主线每个节点 / 每个成就组"隔离：成就组内是同一
 * 条件的递增阶梯，共享计数才能逐级达成；主线相邻节点可能复用同一条件 id 但过滤参数不同
 * (如 12303 分别指定游客卡池与雇员卡池)，必须按节点隔离，否则前一节点的进度会漏给后一节点。
 * 链生命周期(接取/推进/领奖)与持久化由本服务在 sim 内自管；旋转和经营动作都通过统一事实事件推进。
 *
 * @author 11
 * @date 2026/6/25
 */
@Service
public class SimTaskService implements IRedDotService {
    private static final Logger log = LoggerFactory.getLogger(SimTaskService.class);

    //计数 prefix: 主线每个节点一个, 成就每组一个 (featureId = 条件type + prefix)
    private static final String PREFIX_MAIN = "simTaskMain";
    private static final String PREFIX_ACH = "simTaskAch";
    private static final int MAIN_COUNTER_VERSION = 1;
    private static final int MAX_DISPLAYED_MEDALS = 3;
    private static final Set<Integer> TASK_DETAIL_PROGRESS_CONDITIONS = Set.of(
            TaskConstant.ConditionType.PLAYER_BET_ALL,
            12201, 12202, 12203, 12204, 12205, 12206,
            12209, 12210, 12211, 12213, 12215, 12217, 12218,
            12220, 12221, 12222, 12223, 12224, 12225);

    @Autowired
    private SimTaskConfigService taskConfig;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SimPlayerContextRegistry contextRegistry;
    @Autowired
    private RedDotManager redDotManager;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CountDao countDao;
    @Autowired
    private TaskCondition12001 taskCondition12001;
    @Autowired
    private SimPlayerStatService playerStatService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SimMedalService simMedalService;
    @Autowired
    private TaskLogger taskLogger;
    @Autowired
    private GameFunctionService gameFunctionService;
    @Autowired
    private SimGuideService guideService;
    @Autowired
    private SimMainTaskLogger mainTaskLogger;
    @Autowired
    private SimAchievementTaskLogger achievementTaskLogger;
    //@Lazy 打破循环: 本服务 -> 状态补报实现(建筑/游客/雇员/场景) -> 本服务
    @Lazy
    @Autowired(required = false)
    private List<SimTaskStateReporter> stateReporters = Collections.emptyList();

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
        //任务数据就绪后补一次状态: 场景/建筑/雇员/游客在本方法之前加载, 那时的上报会被任务侧丢弃;
        //玩家等级等状态型条件同理, 不补则要等下一次事件或开界面才结算
        settleState(ctx, true);
        //上线即推进"累积登陆天数"(12218): 主线首节点就是该条件, 不接入则整条主线无法起步
        onDailyLogin(ctx);
    }

    /**
     * 状态补报 + 状态轮询: 把"拥有量/总量"型条件的当前值重报一次, 再结算已达标的状态型条件(玩家等级)。
     * 登录与打开任务界面时各执行一次; 重复上报对 SET 语义幂等。
     *
     * @param notify 是否推送变更; 开界面时列表响应本身就带最新状态, 无需再推一次
     */
    private boolean settleState(SimPlayerContext ctx, boolean notify) {
        Player player = resolvePlayer(ctx);
        SimTaskData data = ctx.getSimTaskData();
        if (player == null || data == null) {
            return false;
        }
        List<Task> changed = new ArrayList<>();
        boolean activeNodesChanged;
        do {
            TaskDetail mainBefore = data.getMainTask();
            Map<Integer, TaskDetail> achievementsBefore = new HashMap<>(data.getAchievements());
            for (SimTaskStateReporter reporter : stateReporters) {
                try {
                    reporter.reportTaskState(ctx,
                            event -> tryAdvanceConditionEvent(ctx, event, changed, false, false));
                } catch (Exception e) {
                    log.error("sim 任务状态补报失败 reporter={},playerId={}",
                            reporter.getClass().getSimpleName(), ctx.playerId(), e);
                }
            }
            pollStates(ctx, player, data, ctx.getSimBaseData(), changed, true);
            activeNodesChanged = activeNodesChanged(data, mainBefore, achievementsBefore);
        } while (activeNodesChanged);
        if (notify) {
            notifyChanged(ctx, changed);
        }
        return !changed.isEmpty();
    }

    private boolean activeNodesChanged(SimTaskData data, TaskDetail mainBefore,
                                       Map<Integer, TaskDetail> achievementsBefore) {
        if (mainBefore != data.getMainTask()) {
            return true;
        }
        Map<Integer, TaskDetail> current = data.getAchievements();
        if (achievementsBefore.size() != current.size()) {
            return true;
        }
        for (Map.Entry<Integer, TaskDetail> entry : current.entrySet()) {
            if (achievementsBefore.get(entry.getKey()) != entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 每日登陆推进"累积登陆天数"条件 (12218)。按自然日 (yyyyMMdd) 去重, 同一天多次上线只计一次。
     */
    private void onDailyLogin(SimPlayerContext ctx) {
        SimBaseData baseData = ctx.getSimBaseData();
        if (baseData == null) {
            return;
        }
        int today = TimeHelper.getDayNumerical();
        if (baseData.getLastLoginDay() == today) {
            return;
        }
        baseData.setLastLoginDay(today);
        onConditionEvent(ctx, SimConditionEventFactory.login());
    }

    /**
     * 保证主线与各成就组都有一个"当前节点": 首次接取首节点; 末节点已领取且配置新增了后置则续接。
     */
    private void ensureActive(long playerId, SimTaskData data) {
        if (data.getMainTask() == null) {
            int firstMain = taskConfig.firstMain();
            if (firstMain > 0) {
                TaskDetail first = createNode(playerId, firstMain);
                data.setMainTask(first);
                logMainActivated(playerId, first, 0);
            }
        } else {
            advanceIfRewardedTail(playerId, data, data.getMainTask(), true, 0);
        }
        for (int group : taskConfig.achievementGroupIds()) {
            TaskDetail node = data.getAchievements().get(group);
            if (node == null) {
                int first = taskConfig.firstOf(group);
                if (first > 0) {
                    TaskDetail firstNode = createNode(playerId, first);
                    data.getAchievements().put(group, firstNode);
                    logAchievementActivated(playerId, firstNode, group, 0);
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
                logMainActivated(playerId, next, cur.getConfigId());
            } else {
                data.getAchievements().put(group, next);
                logAchievementActivated(playerId, next, group, cur.getConfigId());
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

    /**
     * GM 将主线或某个成就组向前跳到指定任务。目标必须是当前节点的后置节点，禁止原地设置或回退。
     */
    public CommonResult<String> jumpTask(SimPlayerContext ctx, int taskId) {
        if (ctx == null || ctx.getSimTaskData() == null) {
            return new CommonResult<>(Code.FAIL, "玩家模拟经营任务数据未加载");
        }
        TaskCfg targetCfg = GameDataManager.getTaskCfg(taskId);
        if (targetCfg == null
                || (targetCfg.getTaskType() != TaskConstant.TaskType.MAIN_LINE
                && targetCfg.getTaskType() != TaskConstant.TaskType.ACHIEVEMENT)) {
            return new CommonResult<>(Code.PARAM_ERROR, "任务不存在或不是主线/成就任务：" + taskId);
        }

        SimTaskData data = ctx.getSimTaskData();
        TaskDetail current = findActiveNode(data, targetCfg, taskId);
        if (current == null) {
            return new CommonResult<>(Code.FAIL, "目标任务所属任务链尚未接取：" + taskId);
        }

        int nextId = taskConfig.next(current.getConfigId());
        while (nextId > 0 && nextId != taskId) {
            nextId = taskConfig.next(nextId);
        }
        if (nextId != taskId) {
            return new CommonResult<>(Code.PARAM_ERROR,
                    "只能向前跳转任务，当前任务：" + current.getConfigId() + "，目标任务：" + taskId);
        }

        TaskDetail target = createNode(ctx.playerId(), taskId);
        if (targetCfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            data.setMainTask(target);
        } else {
            data.getAchievements().put(targetCfg.getGroup(), target);
        }
        ctx.setLastSaveTime(0);
        updateTaskRedDot(ctx);
        log.info("GM向前跳转sim任务 playerId={},currentTaskId={},targetTaskId={},taskType={},group={}",
                ctx.playerId(), current.getConfigId(), taskId, targetCfg.getTaskType(), targetCfg.getGroup());
        return new CommonResult<>(Code.SUCCESS,
                "已将当前任务从 " + current.getConfigId() + " 设置为 " + taskId);
    }

    /**
     * GM 强制完成主线/成就任务。未接取的目标会直接成为所属任务链的当前节点并置为待领奖。
     * 指定 null 表示完成主线末节点及每个成就组的末节点。
     */
    public List<Integer> gmFinishTasks(SimPlayerContext ctx, Collection<Integer> specifiedTaskIds) {
        if (ctx == null || ctx.getSimTaskData() == null) {
            log.warn("GM完成sim任务失败，玩家模拟经营任务数据未加载 playerId={}",
                    ctx == null ? 0 : ctx.playerId());
            return null;
        }
        List<TaskCfg> targets;
        if (specifiedTaskIds == null) {
            Map<String, TaskCfg> tails = new HashMap<>();
            for (TaskCfg cfg : GameDataManager.getTaskCfgList()) {
                if (!isSimTaskCfg(cfg) || taskConfig.conditionOf(cfg.getId()) == null) {
                    continue;
                }
                String chainKey = cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE
                        ? "main" : "achievement:" + cfg.getGroup();
                tails.merge(chainKey, cfg,
                        (left, right) -> left.getId() >= right.getId() ? left : right);
            }
            targets = tails.values().stream().sorted(Comparator.comparingInt(TaskCfg::getId)).toList();
        } else {
            LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>(specifiedTaskIds);
            if (uniqueIds.isEmpty()) {
                log.warn("GM完成sim任务失败，指定任务列表为空 playerId={}", ctx.playerId());
                return null;
            }
            targets = new ArrayList<>(uniqueIds.size());
            for (Integer taskId : uniqueIds) {
                TaskCfg cfg = taskId == null ? null : GameDataManager.getTaskCfg(taskId);
                if (!isSimTaskCfg(cfg) || taskConfig.conditionOf(taskId) == null) {
                    log.warn("GM完成sim任务失败，任务不存在或不属于有效主线/成就链 playerId={},taskId={}",
                            ctx.playerId(), taskId);
                    return null;
                }
                targets.add(cfg);
            }
        }

        Player player = resolvePlayer(ctx);
        if (player == null) {
            log.warn("GM完成sim任务失败，玩家数据未加载 playerId={}", ctx.playerId());
            return null;
        }
        SimTaskData data = ctx.getSimTaskData();
        List<Task> changed = new ArrayList<>();
        List<Integer> completed = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TaskCfg cfg : targets) {
            TaskDetail current = findActiveNode(data, cfg, cfg.getId());
            if (current != null && current.getConfigId() > cfg.getId()) {
                continue;
            }
            TaskDetail node = current != null && current.getConfigId() == cfg.getId()
                    ? current : createNode(ctx.playerId(), cfg.getId());
            if (node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
                continue;
            }
            if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
                data.setMainTask(node);
            } else {
                data.getAchievements().put(cfg.getGroup(), node);
            }
            SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
            int conditionId = def.condition().spec().id();
            long target = def.condition().target();
            node.getProgress().put(conditionId, target);
            node.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
            node.setCompleteTime(now);
            if (ctx.getSimBaseData() != null) {
                ctx.getSimBaseData().incFinishedTaskCount();
            }
            taskLogger.completeTask(ctx.playerId(), cfg.getId());
            gameFunctionService.notifyTaskFunctionOpen(ctx.playerId(), List.of(cfg.getFunctionId()));
            if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
                mainTaskLogger.completed(ctx.playerId(), player.getNickName(), cfg.getId(),
                        conditionId, target, target, now);
            } else {
                achievementTaskLogger.completed(ctx.playerId(), player.getNickName(), cfg.getId(), cfg.getGroup(),
                        conditionId, target, target, now);
            }
            changed.add(assemble(ctx, player, node, cfg));
            completed.add(cfg.getId());
        }
        if (!completed.isEmpty()) {
            ctx.setLastSaveTime(0);
            notifyChanged(ctx, changed);
        }
        log.info("GM完成sim任务 playerId={},taskIds={}", ctx.playerId(), completed);
        return completed;
    }

    private boolean isSimTaskCfg(TaskCfg cfg) {
        return cfg != null && (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE
                || cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT);
    }

    // =====================================================================
    // 进度 (旋转事件驱动)
    // =====================================================================

    /** 本节点直接触发的旋转事件沿用完成/续接才推送、纯进度静默的行为。 */
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
            onConditionEvent(ctx, SimConditionEventFactory.fromSpin(gameType,
                    statInfo == null ? 0 : statInfo.getMultiple(), 0, statInfo));
        } catch (Exception e) {
            log.error("sim 任务旋转联动异常 playerId={},gameType={}", ctx.playerId(), gameType, e);
        }
    }

    /**
     * sim 任务的通用事件入口。新增经营动作只需构造 core 条件事件并调用此方法，无需在任务服务
     * 增加 condition id 分支；异常隔离由各事件生产者现有调用链负责。
     */
    public void onConditionEvent(SimPlayerContext ctx, ConditionEvent event) {
        List<Task> changed = new ArrayList<>();
        if (tryAdvanceConditionEvent(ctx, event, changed, true, false)) {
            notifyChanged(ctx, changed);
        }
    }

    /**
     * 跨节点事件入口: 返回本次进度/状态变化，由当前持有客户端会话的调用节点负责通知。
     * 本方法不直接发送消息或更新任务红点，避免使用 sim 登录时保留的旧节点会话。
     */
    public List<Task> collectConditionEventUpdates(SimPlayerContext ctx, ConditionEvent event) {
        List<Task> changed = new ArrayList<>();
        if (!tryAdvanceConditionEvent(ctx, event, changed, true, true) || changed.isEmpty()) {
            return List.of();
        }
        return List.copyOf(changed);
    }

    private boolean tryAdvanceConditionEvent(SimPlayerContext ctx, ConditionEvent event,
                                             List<Task> changed, boolean pollState,
                                             boolean includeProgressChanges) {
        try {
            advanceConditionEvent(ctx, event, changed, pollState, includeProgressChanges);
            return true;
        } catch (Exception e) {
            log.error("sim 任务条件事件异常 playerId={},event={}",
                    ctx == null ? 0 : ctx.playerId(), event, e);
            return false;
        }
    }

    private void advanceConditionEvent(SimPlayerContext ctx, ConditionEvent event,
                                       List<Task> changed, boolean pollState,
                                       boolean includeProgressChanges) {
        if (ctx == null) {
            return;
        }
        SimTaskData data = ctx.getSimTaskData();
        Player player = resolvePlayer(ctx);
        if (data == null || player == null || event == null) {
            //整个玩家的任务推进被丢弃, 不该静默: 任务数据未加载或玩家对象取不到都是异常态
            log.warn("sim 任务事件丢弃 playerId={},taskDataNull={},playerNull={},eventNull={}",
                    ctx.playerId(), data == null, player == null, event == null);
            return;
        }
        evaluateOnEvent(ctx, player, data, ctx.getSimBaseData(), data.getMainTask(), event, changed,
                includeProgressChanges);
        //成就组在事件推进中可能续接(替换同 group 节点), 用 keySet 快照遍历
        for (Integer group : new ArrayList<>(data.getAchievements().keySet())) {
            evaluateOnEvent(ctx, player, data, ctx.getSimBaseData(),
                    data.getAchievements().get(group), event, changed, includeProgressChanges);
        }
        //状态型条件(玩家等级)没有对应的事实事件: 玩家在 slots 节点下注升级, sim 侧收不到升级事件,
        //借任意一次 sim 事件顺带结算, 玩家不必重开任务界面
        if (pollState) {
            pollStates(ctx, player, data, ctx.getSimBaseData(), changed, false);
        }
    }

    private void notifyChanged(SimPlayerContext ctx, List<Task> changed) {
        if (ctx == null || changed.isEmpty()) {
            return;
        }
        updateTaskRedDot(ctx);
        if (ctx.getPlayerController() == null) {
            return;
        }
        NotifySimTaskUpdate notify = new NotifySimTaskUpdate(Code.SUCCESS);
        notify.tasks = changed;
        try {
            ctx.send(notify);
        } catch (Exception e) {
            log.error("推送 sim 任务状态失败 playerId={}", ctx.playerId(), e);
        }
    }

    /**
     * 事件推进单个节点: 累计进度(条件系统) + 判定完成。
     */
    private void evaluateOnEvent(SimPlayerContext ctx, Player player, SimTaskData data,
                                 SimBaseData baseData, TaskDetail node,
                                 ConditionEvent event, List<Task> changed,
                                 boolean includeProgressChanges) {
        if (node == null) {
            return;
        }
        //以下每个 return 都会让"玩家做了动作但进度不动", 逐个说明原因, 便于按 taskId 直接定位卡在哪一环
        if (node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
            return;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
        if (cfg == null) {
            log.warn("任务未推进: 找不到任务配置 playerId={},taskId={}", player.getId(), node.getConfigId());
            return;
        }
        SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
        if (def == null) {
            return;
        }
        if (def.condition().spec().id() == TaskConstant.ConditionType.PLAYER_BET_ALL) {
            evaluateEffectiveBet(ctx, player, data, baseData, node, cfg, event, changed,
                    includeProgressChanges);
            return;
        }
        if (TASK_DETAIL_PROGRESS_CONDITIONS.contains(def.condition().spec().id())) {
            evaluateTaskDetailProgress(ctx, player, data, baseData, node, cfg, def.condition(), event, changed,
                    includeProgressChanges);
            return;
        }
        if (playerStatService.supports(def.condition())) {
            ConditionUpdate update = def.condition().evaluate(event);
            boolean relevant = update.matched()
                    || (def.condition().spec().id() == PlayerStatService.FREE_MODE
                    && event instanceof GameConditionEvent);
            if (!relevant) {
                return;
            }
            long progress = playerStatService.progress(ctx, def.condition());
            if (progress >= def.condition().target()) {
                onComplete(ctx, player, data, baseData, node, cfg, changed);
            } else if (includeProgressChanges && update.matched() && update.value() > 0) {
                changed.add(assemble(ctx, player, node, cfg, progress));
            }
            return;
        }
        ConditionUpdate update = def.condition().evaluate(event);
        if (!update.matched() || update.value() <= 0) {
            return;
        }
        String featureId = def.counterType() + prefixOf(cfg);
        String customId = String.valueOf(player.getId());
        BigDecimal value = BigDecimal.valueOf(update.value());
        long progress = switch (update.mode()) {
            case ADD -> countDao.incrBy(player.getId(), featureId, customId, value).longValue();
            case MAX -> countDao.max(player.getId(), featureId, customId, value).longValue();
            case SET -> {
                countDao.setCount(player.getId(), featureId, customId, value);
                yield update.value();
            }
        };
        if (log.isDebugEnabled()) {
            //带上计数 key: 进度存疑时可直接 redis-cli get 该 key 核对 (存的是实际值 x100)
            log.debug("玩家[{}]任务[{}]条件[{}]推进 增量={},进度={}/{},计数key=count:{}:{}",
                    player.getId(), cfg.getId(), def.condition().spec().id(), update.value(),
                    progress, def.condition().target(), featureId, customId);
        }
        if (progress >= def.condition().target()) {
            onComplete(ctx, player, data, baseData, node, cfg, changed);
        } else if (includeProgressChanges) {
            changed.add(assemble(ctx, player, node, cfg, progress));
        }
    }

    /** 12001 沿用旧任务条件的任务内进度，只累计当前已接取节点收到的有效下注事件。 */
    private void evaluateEffectiveBet(SimPlayerContext ctx, Player player, SimTaskData data,
                                      SimBaseData baseData, TaskDetail node, TaskCfg cfg,
                                      ConditionEvent event, List<Task> changed,
                                      boolean includeProgressChanges) {
        if (!(event instanceof GameConditionEvent gameEvent) || gameEvent.bet() <= 0) {
            return;
        }
        TaskConditionParam12001 param = new TaskConditionParam12001();
        param.setGameId(gameEvent.gameId());
        param.setAddValue(gameEvent.bet());
        if (!taskCondition12001.trigger(player.getId(), cfg, node, param)) {
            return;
        }
        ctx.setLastSaveTime(0);
        if (node.getFinishConditionIds().contains(TaskConstant.ConditionType.PLAYER_BET_ALL)) {
            onComplete(ctx, player, data, baseData, node, cfg, changed);
        } else if (includeProgressChanges) {
            changed.add(assemble(ctx, player, node, cfg));
        }
    }

    /** 接取后累计型条件直接保存在当前任务节点中，不继承接取前的任何历史进度。 */
    private void evaluateTaskDetailProgress(SimPlayerContext ctx, Player player, SimTaskData data,
                                            SimBaseData baseData, TaskDetail node, TaskCfg cfg,
                                            PreparedCondition condition, ConditionEvent event,
                                            List<Task> changed, boolean includeProgressChanges) {
        ConditionUpdate update = condition.evaluate(event);
        if (!update.matched() || update.value() <= 0) {
            return;
        }
        int conditionId = condition.spec().id();
        long current = node.getProgress().getOrDefault(conditionId, 0L);
        long progress = update.apply(current);
        if (progress == current) {
            return;
        }
        node.getProgress().put(conditionId, progress);
        ctx.setLastSaveTime(0);
        if (progress >= condition.target()) {
            node.getFinishConditionIds().add(conditionId);
            onComplete(ctx, player, data, baseData, node, cfg, changed);
        } else if (includeProgressChanges) {
            changed.add(assemble(ctx, player, node, cfg));
        }
    }

    /**
     * 轮询主线与各成就组的当前节点。成就组可能在轮询中续接(替换同 group 节点), 用 keySet 快照遍历。
     */
    private void pollStates(SimPlayerContext ctx, Player player, SimTaskData data,
                            SimBaseData baseData, List<Task> changed, boolean includePlayerStats) {
        pollState(ctx, player, data, baseData, data.getMainTask(), changed, includePlayerStats);
        for (Integer group : new ArrayList<>(data.getAchievements().keySet())) {
            pollState(ctx, player, data, baseData, data.getAchievements().get(group),
                    changed, includePlayerStats);
        }
    }

    /**
     * 状态轮询(无事件): 开界面/上线时结算已达标的节点(覆盖玩家等级提升、计数已够等场景)。
     */
    private void pollState(SimPlayerContext ctx, Player player, SimTaskData data,
                           SimBaseData baseData, TaskDetail node, List<Task> changed,
                           boolean includePlayerStats) {
        if (node == null || node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
            return;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
        if (cfg == null) {
            return;
        }
        SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
        if (def != null && playerStatService.supports(def.condition())) {
            if (includePlayerStats
                    && playerStatService.progress(ctx, def.condition()) >= def.condition().target()) {
                onComplete(ctx, player, data, baseData, node, cfg, changed);
            }
            return;
        }
        StateConditionEvent event = def == null ? null : playerState(player, def.condition());
        if (event != null && def.condition().evaluate(event).apply(0) >= def.condition().target()) {
            onComplete(ctx, player, data, baseData, node, cfg, changed);
        }
    }

    // =====================================================================
    // 完成 / 领奖 / 推进
    // =====================================================================

    /**
     * 节点完成: 置完成态; 无奖励则直接领取并续接, 有奖励则等待客户端领取。
     */
    private void onComplete(SimPlayerContext ctx, Player player, SimTaskData data,
                            SimBaseData baseData, TaskDetail node, TaskCfg cfg, List<Task> changed) {
        long now = System.currentTimeMillis();
        SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
        int conditionId = def == null ? 0 : def.condition().spec().id();
        long target = def == null ? 0 : def.condition().target();
        long completedProgress = def == null ? 0 : currentProgress(ctx, player, node, cfg);
        if (def != null) {
            node.getProgress().put(conditionId, completedProgress);
        }
        node.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
        node.setCompleteTime(now);
        if (baseData != null) {
            baseData.incFinishedTaskCount();
        }
        //后台任务日志: 完成 (主线/成就类型由后台按 taskType 区分)
        taskLogger.completeTask(player.getId(), node.getConfigId());
        log.info("玩家[{}]完成 sim 任务[{}]", player.getId(), node.getConfigId());
        gameFunctionService.notifyTaskFunctionOpen(player.getId(), List.of(cfg.getFunctionId()));
        if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            mainTaskLogger.completed(player.getId(), player.getNickName(), cfg.getId(),
                    conditionId, completedProgress, target, now);
        } else if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
            achievementTaskLogger.completed(player.getId(), player.getNickName(), cfg.getId(), cfg.getGroup(),
                    conditionId, completedProgress, target, now);
        }

        boolean noReward = (cfg.getGetItem() == null || cfg.getGetItem().isEmpty())
                && cfg.getIntegralNum() <= TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD;
        if (noReward) {
            node.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
            node.setRewardTime(now);
            taskLogger.receiveTaskAward(player.getId(), node.getConfigId(), null,
                    cfg.getIntegralNum(), TaskConstant.TaskStatus.STATUS_REWARDED);
            changed.add(assemble(ctx, player, node, cfg));
            if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
                int nextTaskId = taskConfig.next(cfg.getId());
                mainTaskLogger.rewarded(player.getId(), player.getNickName(), cfg.getId(),
                        Collections.emptyList(), nextTaskId, now);
                if (nextTaskId <= 0) {
                    mainTaskLogger.allCompleted(player.getId(), player.getNickName(), cfg.getId(), now);
                }
            } else if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
                int nextTaskId = taskConfig.next(cfg.getId());
                achievementTaskLogger.rewarded(player.getId(), player.getNickName(), cfg.getId(), cfg.getGroup(),
                        Collections.emptyList(), nextTaskId, now);
                if (nextTaskId <= 0) {
                    achievementTaskLogger.allCompleted(player.getId(), player.getNickName(), cfg.getId(),
                            cfg.getGroup(), now);
                }
            }
            Task next = advance(ctx, player, data, node, cfg);
            if (next != null) {
                changed.add(next);
            }
        } else {
            taskLogger.receiveTaskAward(player.getId(), node.getConfigId(), null,
                    cfg.getIntegralNum(), TaskConstant.TaskStatus.STATUS_COMPLETED);
            changed.add(assemble(ctx, player, node, cfg));
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
            CommonResult<ItemOperationResult> addResult = playerPackService.addItems(
                    ctx.playerId(), cfg.getGetItem(), AddType.TASKAWARD, "taskId=" + taskId, cfg.getTaskType() != TaskConstant.TaskType.MAIN_LINE);
            if (addResult == null || !addResult.success()) {
                res.code = addResult == null ? Code.EXCEPTION : addResult.code;
                log.error("领取 sim 任务奖励失败,发奖失败 playerId={},taskId={},result={}",
                        playerId, taskId, addResult);
                return res;
            }
            rewardItems = toItemList(cfg.getGetItem());
        }
        node.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
        long rewardTime = System.currentTimeMillis();
        node.setRewardTime(rewardTime);
        //后台任务日志: 奖励领取 (含领取内容)
        taskLogger.receiveTaskAward(playerId, taskId, rewardItems, cfg.getIntegralNum(),
                TaskConstant.TaskStatus.STATUS_REWARDED);

        res.taskId = taskId;
        if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            int nextTaskId = taskConfig.next(taskId);
            mainTaskLogger.rewarded(playerId, player.getNickName(), taskId,
                    rewardItems, nextTaskId, rewardTime);
            if (nextTaskId <= 0) {
                mainTaskLogger.allCompleted(playerId, player.getNickName(), taskId, rewardTime);
            }
        } else if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
            int nextTaskId = taskConfig.next(taskId);
            achievementTaskLogger.rewarded(playerId, player.getNickName(), taskId, cfg.getGroup(),
                    rewardItems, nextTaskId, rewardTime);
            if (nextTaskId <= 0) {
                achievementTaskLogger.allCompleted(playerId, player.getNickName(), taskId,
                        cfg.getGroup(), rewardTime);
            }
            //领取成功后、激活同组下一节点前触发，确保只有本次领取时已经接取的任务能够累计。
            onConditionEvent(ctx, new ActionConditionEvent(
                    ActionConditionEvent.Type.ACHIEVEMENT_REWARD, 0, 0, 0, 1, 0, false));
        }
        res.nextTask = advance(ctx, player, data, node, cfg);
        if (res.nextTask != null) {
            res.nextTask = settleAdvancedChain(ctx, player, data, cfg);
        }
        res.rewards = ItemUtils.buildItemInfosByItem(rewardItems);
        //领奖后强制下个 tick 尽快落库(走 autosave 规范路径), 收窄崩溃重复领取窗口
        ctx.setLastSaveTime(0);
        updateTaskRedDot(ctx);
        //成就任务末节点奖励含勋章道具, 领取后可能新激活勋章 -> 刷新全服勋章榜分值
        if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
            simMedalService.refreshRankScore(ctx);
            simMedalService.refreshMedalBonusCache(ctx);
        }
        guideService.trigger(ctx, com.jjg.game.sim.constant.SimConstant.GuideCondition.TASK_REWARD, taskId, true);
        log.info("玩家[{}]领取 sim 任务[{}]奖励成功", playerId, taskId);
        return res;
    }

    /**
     * 链推进: 末节点保持已领取态(主线"任务预告"/成就组完成); 否则新建并激活下一节点。
     *
     * @return 新激活的下一节点协议体, 无后置返回 null
     */
    private Task advance(SimPlayerContext ctx, Player player, SimTaskData data,
                         TaskDetail node, TaskCfg cfg) {
        int nextId = taskConfig.next(node.getConfigId());
        if (nextId <= 0) {
            return null;
        }
        TaskDetail next = createNode(player.getId(), nextId);
        if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
            data.setMainTask(next);
            logMainActivated(player.getId(), next, node.getConfigId());
        } else {
            data.getAchievements().put(cfg.getGroup(), next);
            logAchievementActivated(player.getId(), next, cfg.getGroup(), node.getConfigId());
        }
        TaskCfg nextCfg = GameDataManager.getTaskCfg(nextId);
        return nextCfg == null ? null : assemble(ctx, player, next, nextCfg);
    }

    /**
     * 领奖续接后立即结算同一条任务链，避免下一节点的历史/状态进度已达标但仍返回进行中。
     * 无奖励节点完成时会在 {@link #onComplete} 中继续续接，因此循环到当前节点稳定为止。
     */
    private Task settleAdvancedChain(SimPlayerContext ctx, Player player, SimTaskData data, TaskCfg chainCfg) {
        List<Task> changed = new ArrayList<>();
        SimBaseData baseData = ctx.getSimBaseData();
        TaskDetail node;
        do {
            node = findActiveNode(data, chainCfg, chainCfg.getId());
            if (node == null) {
                return null;
            }
            TaskDetail activatedNode = node;
            for (SimTaskStateReporter reporter : stateReporters) {
                try {
                    reporter.reportTaskState(ctx, event -> {
                        try {
                            evaluateOnEvent(ctx, player, data, baseData, activatedNode, event, changed, false);
                        } catch (Exception e) {
                            log.error("sim 新接取任务状态补报失败 reporter={},playerId={},taskId={}",
                                    reporter.getClass().getSimpleName(), ctx.playerId(), activatedNode.getConfigId(), e);
                        }
                    });
                } catch (Exception e) {
                    log.error("sim 新接取任务状态补报失败 reporter={},playerId={},taskId={}",
                            reporter.getClass().getSimpleName(), ctx.playerId(), activatedNode.getConfigId(), e);
                }
            }
            pollState(ctx, player, data, baseData, activatedNode, changed, true);
            if (activatedNode.getStatus() == TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
                TaskCfg activatedCfg = GameDataManager.getTaskCfg(activatedNode.getConfigId());
                SimTaskConfigService.TaskConditionDef def = activatedCfg == null
                        ? null : taskConfig.conditionOf(activatedCfg.getId());
                if (def != null && currentProgress(ctx, player, activatedNode, activatedCfg) >= def.condition().target()) {
                    onComplete(ctx, player, data, baseData, activatedNode, activatedCfg, changed);
                }
            }
        } while (node != findActiveNode(data, chainCfg, chainCfg.getId()));

        TaskDetail active = findActiveNode(data, chainCfg, chainCfg.getId());
        TaskCfg activeCfg = active == null ? null : GameDataManager.getTaskCfg(active.getConfigId());
        return activeCfg == null ? null : assemble(ctx, player, active, activeCfg);
    }

    /**
     * 打开任务界面时记录主线当前节点快照。
     * <p>
     * 客户端按配置把整条主线都渲染出来, 服务端只跟踪一个"当前节点", 所以"某任务进度不涨"最常见的原因是
     * 它根本还没轮到。这行日志直接给出当前节点 id / 状态 / 进度 / 计数 key, 一眼分辨是没轮到还是真没推进。
     */
    private void logMainSnapshot(SimPlayerContext ctx, Player player, TaskDetail main) {
        if (main == null) {
            log.info("主线当前节点: 无 (任务链为空或配置未加载) playerId={}", player.getId());
            return;
        }
        TaskCfg cfg = GameDataManager.getTaskCfg(main.getConfigId());
        SimTaskConfigService.TaskConditionDef def = cfg == null ? null : taskConfig.conditionOf(cfg.getId());
        log.info("主线当前节点: taskId={},status={},进度={}/{},condition={},进度来源={}",
                main.getConfigId(), main.getStatus(),
                cfg == null ? -1 : currentProgress(ctx, player, main, cfg),
                def == null ? -1 : def.condition().target(),
                cfg == null ? "配置缺失" : cfg.getTaskConditionId(),
                def == null || cfg == null ? "无"
                        : TASK_DETAIL_PROGRESS_CONDITIONS.contains(def.condition().spec().id())
                        ? "SimTaskData.progress"
                        : playerStatService.supports(def.condition())
                        ? CountDao.CountType.PLAYER_STAT.getParam().formatted(def.condition().spec().id())
                        : "count:" + def.counterType() + prefixOf(cfg) + ":" + player.getId());
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
        //开界面时顺带补齐(配置热更新增成就组/主线续接) + 状态补报与轮询结算
        //本次响应就带上最新状态, 无需再推送 NotifySimTaskUpdate
        ensureActive(player.getId(), data);
        if (settleState(ctx, false)) {
            updateTaskRedDot(ctx);
        }

        TaskDetail main = data.getMainTask();
        if (main != null) {
            TaskCfg mainCfg = GameDataManager.getTaskCfg(main.getConfigId());
            if (mainCfg != null) {
                res.mainTask = assemble(ctx, player, main, mainCfg);
                res.mainFinished = main.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED
                        && taskConfig.next(main.getConfigId()) <= 0;
            }
        }
        //客户端按配置渲染整条链, 服务端只推进"当前节点": 排查"某个任务进度不动"先看这行是不是那个 taskId
        logMainSnapshot(ctx, player, main);
        List<Task> achievements = new ArrayList<>(data.getAchievements().size());
        for (TaskDetail node : data.getAchievements().values()) {
            TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
            if (cfg != null) {
                achievements.add(assemble(ctx, player, node, cfg));
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

    // =====================================================================
    // 红点
    // =====================================================================

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.TASK;
    }

    @Override
    public List<Integer> getSubmodules() {
        return List.of(TaskConstant.TaskType.MAIN_LINE, TaskConstant.TaskType.ACHIEVEMENT);
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        Map<Integer, Integer> counts = ctx != null && ctx.getSimTaskData() != null
                ? claimableCounts(ctx.getSimTaskData())
                : simTaskDao.findClaimableCounts(playerId);
        return buildRedDotDetails(counts, submodule);
    }

    private void updateTaskRedDot(SimPlayerContext ctx) {
        try {
            redDotManager.updateRedDot(
                    buildRedDotDetails(claimableCounts(ctx.getSimTaskData()), TaskConstant.TimeConstants.ALL_SUBMODULES),
                    ctx.playerId());
        } catch (Exception e) {
            log.error("更新任务红点失败 playerId={}", ctx.playerId(), e);
        }
    }

    private List<RedDotDetails> buildRedDotDetails(Map<Integer, Integer> counts, int submodule) {
        List<RedDotDetails> details = new ArrayList<>(2);
        for (Integer taskType : getSubmodules()) {
            if (submodule == TaskConstant.TimeConstants.ALL_SUBMODULES || submodule == taskType) {
                details.add(redDotManager.buildRedDotDetails(
                        getModule(), taskType, counts.getOrDefault(taskType, 0)));
            }
        }
        return details;
    }

    private Map<Integer, Integer> claimableCounts(SimTaskData data) {
        int mainCount = data.getMainTask() != null
                && data.getMainTask().getStatus() == TaskConstant.TaskStatus.STATUS_COMPLETED ? 1 : 0;
        int achievementCount = (int) data.getAchievements().values().stream()
                .filter(task -> task.getStatus() == TaskConstant.TaskStatus.STATUS_COMPLETED)
                .count();
        return Map.of(
                TaskConstant.TaskType.MAIN_LINE, mainCount,
                TaskConstant.TaskType.ACHIEVEMENT, achievementCount);
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
     * 组装任务协议体: configId + 状态 + 奖励道具 + 单条件(当前进度/目标/是否完成)。
     * 进行中节点回读实时进度，完成节点返回完成时保存的进度快照。
     */
    private Task assemble(SimPlayerContext ctx, Player player, TaskDetail node, TaskCfg cfg) {
        return assemble(ctx, player, node, cfg, null);
    }

    /** 复用事件推进时已取得的进度，避免跨节点通知为组装协议再读一次 Redis。 */
    private Task assemble(SimPlayerContext ctx, Player player, TaskDetail node, TaskCfg cfg,
                          Long progressOverride) {
        Task task = new Task();
        task.setConfigId(cfg.getId());
        task.setStatus(node.getStatus());
        task.rewards = ItemUtils.buildItemInfo(cfg.getGetItem());

        List<Long> cond = cfg.getTaskConditionId();
        TaskCondition c = new TaskCondition();
        c.setConfigId(cond.getFirst().intValue());
        SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
        c.setConfigParam(def == null ? cond.getLast() : def.condition().target());
        c.setProgress(node.getStatus() == TaskConstant.TaskStatus.STATUS_IN_PROGRESS
                ? (progressOverride == null ? currentProgress(ctx, player, node, cfg) : progressOverride)
                : completedProgress(node));
        c.setFinish(node.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        task.getConditions().add(c);
        return task;
    }

    private static long completedProgress(TaskDetail node) {
        return node.getProgress().values().stream().findFirst().orElse(0L);
    }

    /**
     * 当前进度: 接取后累计型取任务节点内进度，玩家统计型取玩家级统计，状态型取实时状态，其余事件型从任务计数器回读。
     */
    private long currentProgress(SimPlayerContext ctx, Player player, TaskDetail node, TaskCfg cfg) {
        SimTaskConfigService.TaskConditionDef def = taskConfig.conditionOf(cfg.getId());
        if (def == null) {
            return 0;
        }
        if (TASK_DETAIL_PROGRESS_CONDITIONS.contains(def.condition().spec().id())) {
            return node.getProgress().getOrDefault(def.condition().spec().id(), 0L);
        }
        if (playerStatService.supports(def.condition())) {
            return playerStatService.progress(ctx, def.condition());
        }
        StateConditionEvent state = playerState(player, def.condition());
        if (state != null) {
            return def.condition().evaluate(state).apply(0);
        }
        return countDao.getCount(def.counterType() + prefixOf(cfg), String.valueOf(player.getId())).longValue();
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /**
     * 首次升级到按节点隔离的主线计数时, 把旧共享 key 的可见进度快照到当前节点。
     * 迁移版本随任务文档持久化, 确保后续节点不会再次继承旧共享计数。
     */
    private void migrateMainCounter(long playerId, SimTaskData data) {
        if (data.getMainCounterVersion() >= MAIN_COUNTER_VERSION) {
            return;
        }
        try {
            TaskDetail node = data.getMainTask();
            if (node == null) {
                data.setMainCounterVersion(MAIN_COUNTER_VERSION);
                return;
            }
            TaskCfg cfg = GameDataManager.getTaskCfg(node.getConfigId());
            SimTaskConfigService.TaskConditionDef def = cfg == null ? null : taskConfig.conditionOf(cfg.getId());
            if (cfg == null || def == null || cfg.getTaskType() != TaskConstant.TaskType.MAIN_LINE) {
                log.warn("暂缓迁移 sim 主线计数,任务配置未就绪 playerId={},taskId={}",
                        playerId, node.getConfigId());
                return;
            }
            String customId = String.valueOf(playerId);
            String oldFeatureId = def.counterType() + PREFIX_MAIN;
            String newFeatureId = def.counterType() + prefixOf(cfg);
            if (!countDao.exists(newFeatureId, customId)
                    && countDao.exists(oldFeatureId, customId)) {
                BigDecimal progress = countDao.getCount(oldFeatureId, customId);
                countDao.setCount(playerId, newFeatureId, customId, progress);
                log.info("玩家[{}]迁移 sim 主线任务[{}]计数进度={}",
                        playerId, cfg.getId(), progress.longValue());
            }
            data.setMainCounterVersion(MAIN_COUNTER_VERSION);
        } catch (Exception e) {
            log.error("迁移 sim 主线计数失败 playerId={}", playerId, e);
        }
    }

    /**
     * 计数隔离 prefix: 主线按节点隔离, 成就每组一个 (组内阶梯共享计数以逐级达成)。
     */
    private String prefixOf(TaskCfg cfg) {
        return cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE
                ? PREFIX_MAIN + cfg.getId()
                : PREFIX_ACH + cfg.getGroup();
    }

    private void logMainActivated(long playerId, TaskDetail node, int previousTaskId) {
        Player player = corePlayerService.get(playerId);
        String playerName = player == null ? "" : player.getNickName();
        mainTaskLogger.activated(playerId, playerName, node.getConfigId(),
                previousTaskId, taskConfig.next(node.getConfigId()), node.getCreateTime());
    }

    private void logAchievementActivated(long playerId, TaskDetail node, int group, int previousTaskId) {
        Player player = corePlayerService.get(playerId);
        String playerName = player == null ? "" : player.getNickName();
        achievementTaskLogger.activated(playerId, playerName, node.getConfigId(), group,
                previousTaskId, taskConfig.next(node.getConfigId()), node.getCreateTime());
    }

    private StateConditionEvent playerState(Player player, PreparedCondition condition) {
        return SimTaskStateEventFactory.from(player, condition);
    }

    private Player resolvePlayer(SimPlayerContext ctx) {
        PlayerController pc = ctx.getPlayerController();
        if (pc != null && pc.getPlayer() != null) {
            return pc.getPlayer();
        }
        return corePlayerService.get(ctx.playerId());
    }
}
