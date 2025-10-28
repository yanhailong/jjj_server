package com.jjg.game.core.task.service;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.rpc.HallPointsAwardBridge;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.condition.AbstractTaskCondition;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDataDao;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.core.task.pb.res.NotifyUpdateTask;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 任务服务
 * 负责玩家任务的初始化、进度更新、完成检查和奖励发放
 */
@Service
public class TaskService implements IRedDotService, IPlayerLoginSuccess, GameEventListener {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final ClusterSystem clusterSystem;
    private final TaskLogger taskLogger;
    private final RedDotManager redDotManager;
    private final RedissonClient redissonClient;
    private final TaskDataDao taskDataDao;
    private final RedisLock redisLock;
    private final PlayerPackService playerPackService;
    @ClusterRpcReference()
    private HallPointsAwardBridge hallPointsAwardBridge;

    /**
     * 锁时间。
     */
    private static final int LOCK_TIME = 10000;

    /**
     * 任务管理器 任务服务初始化后才赋值
     */
    private TaskManager taskManager;

    public TaskService(ClusterSystem clusterSystem,
                       RedDotManager redDotManager,
                       RedissonClient redissonClient,
                       TaskDataDao taskDataDao,
                       RedisLock redisLock,
                       TaskLogger taskLogger,
                       @Lazy PlayerPackService playerPackService) {
        this.clusterSystem = clusterSystem;
        this.redDotManager = redDotManager;
        this.redissonClient = redissonClient;
        this.taskDataDao = taskDataDao;
        this.redisLock = redisLock;
        this.taskLogger = taskLogger;
        this.playerPackService = playerPackService;
    }

    /**
     * 初始化
     */
    public void init(TaskManager taskManager) {
        this.taskManager = taskManager;
        log.info("task service init");
    }

    /**
     * 玩家任务map的redis key
     */
    public String playerTaskMapKey(long playerId) {
        return TaskConstant.RedisKey.TASK_PLAYER_MAP + playerId;
    }

    /**
     * 获取玩家的任务Map
     *
     * @param playerId 玩家ID
     * @return 任务数据Map
     */
    public RMap<Integer, TaskData> getPlayerTaskMap(long playerId) {
        return redissonClient.getMap(playerTaskMapKey(playerId));
    }

    /**
     * 玩家登录成功事件
     *
     * @param playerController 玩家信息
     * @param player
     * @param firstLogin       是否是首次登录
     * @return true 继续执行 false终止执行
     */
    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, boolean firstLogin) {
        long playerId = player.getId();
        try {
            //加载玩家所有任务
            loadTasks(playerId);
            //检测玩家任务是否过期并且领取新任务
            checkTask(playerId);
        } catch (Exception e) {
            log.error("初始化玩家任务失败 playerId={}, firstLogin={}, error={}", playerId, firstLogin, e.getMessage(), e);
        }
    }

    /**
     * 玩家任务map锁
     *
     * @param playerId 玩家id
     */
    public String playerTaskMapLockKey(long playerId) {
        return TaskConstant.RedisLockKey.TASK_PLAYER_MAP_LOCK + playerId;
    }

    /**
     * 同步覆盖玩家缓存任务数据
     *
     * @param playerId 玩家id
     * @param taskData 覆盖的任务数据
     */
    public void updatePlayerTaskCache(long playerId, TaskData taskData) {
        try {
            RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
            redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
                //同步到缓存中
                playerTasks.fastPut(taskData.getConfigId(), taskData);
            });
            //同步到数据库
            taskDataDao.save(taskData);
        } catch (Exception e) {
            log.error("更新玩家任务缓存失败 playerId={}, taskId={}, error={}",
                    playerId, taskData.getConfigId(), e.getMessage(), e);
            throw new RuntimeException("更新任务缓存失败", e);
        }
    }

    /**
     * 触发玩家任务
     *
     * @param playerId    玩家id
     * @param taskCfgList 需要触发的任务配置列表
     * @param param       参数
     */
    public void trigger(long playerId, List<TaskCfg> taskCfgList, AbstractTaskCondition<DefaultTaskConditionParam> condition, DefaultTaskConditionParam param) {
        RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
        TaskData taskData;
        log.info("玩家[{}]触发条件[{}]conditionId[{}]参数[{}]", playerId, condition.getClass().getSimpleName(), condition.getId(), param == null ? "null" : param.toString());
        for (TaskCfg taskCfg : taskCfgList) {
            //接了任务才触发
            taskData = playerTasks.get(taskCfg.getId());
            if (taskData != null) {
                TaskData data = redisLock.lockAndGet(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
                    //从内存加载最新的任务对象进行处理
                    TaskData tempData = playerTasks.get(taskCfg.getId());
                    boolean isTriggered = condition.trigger(taskCfg, tempData, param);
                    log.info("玩家[{}]触发任务[{}]条件[{}]触发[{}]", playerId, taskCfg.getId(), condition.getClass().getSimpleName(), isTriggered);
                    if (isTriggered) {
                        LocalDateTime now = LocalDateTime.now();
                        long timestamp = TimeHelper.getTimestamp(now);
                        //任务所有条件都完成了
                        if (tempData.getFinishConditionIds().size() == tempData.getProgress().size()) {
                            tempData.setCompleteTime(timestamp);
                            //检测是否有奖励
                            List<Integer> awardList = taskCfg.getGetItem();
                            //没有奖励的任务 默认直接完成并且已经领取奖励
                            if (awardList.isEmpty() && taskCfg.getIntegralNum() <= TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD) {
                                tempData.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
                                tempData.setRewardTime(timestamp);
                                taskLogger.receiveTaskAward(playerId, tempData.getConfigId(), null, taskCfg.getIntegralNum());
                                log.info("玩家[{}]完成任务[{}]任务没有奖励直接修改为已领取状态!", playerId, tempData.getConfigId());
                            } else {
                                tempData.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
                                taskLogger.completeTask(playerId, tempData.getConfigId());
                                log.info("玩家[{}]完成任务[{}]", playerId, tempData.getConfigId());
                            }
                        }
                        return tempData;
                    }
                    return null;
                });
                if (data != null) {
                    //同步到缓存中
                    playerTasks.fastPut(data.getConfigId(), data);
                    //任务条件完成了 通知红点
                    redDotManager.updateRedDot(this, taskCfg.getTaskType(), playerId);
                    //通知进度更新
                    noticeUpdate(playerId, data, taskCfg);
                    //更新到数据库中
                    taskDataDao.save(data);
                }
            }
        }
    }

    /**
     * 加载玩家任务 内存中没有则从数据库加载 缓存存在则直接返回缓存数据
     *
     * @param playerId 玩家id
     */
    public void loadTasks(long playerId) {
        try {
            RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
            List<TaskData> tasks;
            tasks = playerTasks.values().stream().toList();
            if (tasks.isEmpty()) {
                tasks = taskDataDao.findByPlayerId(playerId);
                if (tasks != null) {
                    for (TaskData taskData : tasks) {
                        playerTasks.fastPut(taskData.getConfigId(), taskData);
                    }
                    log.info("从数据库加载玩家[{}]任务数量: {}", playerId, tasks.size());
                }
            } else {
                log.debug("从缓存加载玩家[{}]任务数量: {}", playerId, tasks.size());
            }
        } catch (Exception e) {
            log.error("加载玩家任务失败 playerId={}, error={}", playerId, e.getMessage(), e);
            throw new RuntimeException("加载玩家任务失败", e);
        }
    }

    /**
     * 判断任务是否应该刷新（是否跨越了12点或24点）
     *
     * @param createTime 任务创建时间
     * @return true-需要刷新，false-不需要刷新
     */
    private boolean shouldRefreshTask(LocalDateTime createTime) {
        LocalDateTime now = LocalDateTime.now();
        // 不是同一天，肯定需要刷新
        if (!createTime.toLocalDate().isEqual(now.toLocalDate())) {
            return true;
        }
        // 同一天的情况下，判断是否跨越了12点
        int createHour = createTime.getHour();
        int nowHour = now.getHour();
        // 创建时间在0-11点（上半天），当前时间在12-23点（下半天）
        return createHour < TaskConstant.TimeConstants.NOON_HOUR && nowHour >= TaskConstant.TimeConstants.NOON_HOUR;
    }

    /**
     * 检测玩家任务
     */
    public void checkTask(long playerId) {
        redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
            RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
            boolean noticeRedDot = false;

            // 删除过期任务
            noticeRedDot |= removeExpiredTasks(playerId, playerTasks);

            // 添加新任务
            noticeRedDot |= addNewTasks(playerId, playerTasks);

            if (noticeRedDot) {
                //更新红点信息
                redDotManager.updateRedDot(this, 0, playerId);
            }
        });
    }

    /**
     * 删除过期任务
     *
     * @param playerId    玩家ID
     * @param playerTasks 玩家任务Map
     * @return 是否需要更新红点
     */
    private boolean removeExpiredTasks(long playerId, RMap<Integer, TaskData> playerTasks) {
        List<TaskData> deletedTasks = new ArrayList<>();

        try {
            for (Map.Entry<Integer, TaskData> entry : playerTasks.entrySet()) {
                Integer taskId = entry.getKey();
                TaskData taskData = entry.getValue();
                TaskCfg taskCfg = GameDataManager.getTaskCfg(taskId);

                boolean shouldDelete = false;
                //有配置才处理
                if (taskCfg != null) {
                    //检测任务是否需要删除
                    if (shouldDelete(taskData, taskCfg)) {
                        shouldDelete = true;
                    }
                } else {
                    //配置不存在则删除任务
                    shouldDelete = true;
                    log.warn("任务配置不存在，将删除任务 playerId={}, taskId={}", playerId, taskId);
                }

                if (shouldDelete) {
                    deletedTasks.add(taskData);
                }
            }

            //有需要删除的过期任务
            if (!deletedTasks.isEmpty()) {
                //删除数据库
                taskDataDao.deleteAllList(deletedTasks);
                for (TaskData taskData : deletedTasks) {
                    playerTasks.fastRemove(taskData.getConfigId());
                    log.info("玩家[{}]任务[{}]过期!", playerId, taskData.getConfigId());
                }
                return true;
            }
        } catch (Exception e) {
            log.error("删除过期任务失败 playerId={}, error={}", playerId, e.getMessage(), e);
        }

        return false;
    }

    /**
     * 添加新任务
     *
     * @param playerId    玩家ID
     * @param playerTasks 玩家任务Map
     * @return 是否需要更新红点
     */
    private boolean addNewTasks(long playerId, RMap<Integer, TaskData> playerTasks) {
        // 获取当前可接取的任务配置列表
        List<TaskCfg> availableTaskConfigs = getAvailableTaskConfigs();
        // 筛选出需要新增的任务
        List<TaskData> newTasks = createNewTasksForPlayer(playerId, playerTasks, availableTaskConfigs);
        //有任务才更新
        if (!newTasks.isEmpty()) {
            //覆盖数据库数据
            taskDataDao.saveAll(newTasks);
            //覆盖缓存
            newTasks.forEach(taskData -> {
                playerTasks.fastPut(taskData.getConfigId(), taskData);
                //记录领取任务日志
                taskLogger.receiveTask(playerId, taskData.getConfigId());
                log.info("玩家[{}]领取到[{}]任务", playerId, taskData.getConfigId());
            });
            return true;
        }
        return false;
    }

    /**
     * 获取当前可接取的任务配置列表
     *
     * @return 可接取的任务配置列表
     */
    private List<TaskCfg> getAvailableTaskConfigs() {
        List<TaskCfg> taskCfgList = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        // 按组分组所有任务配置
        Map<Integer, List<TaskCfg>> taskConfigsByGroupId = GameDataManager.getTaskCfgList().stream()
                .collect(Collectors.groupingBy(TaskCfg::getGroup));

        //筛选出每个组中可以接取的任务
        taskConfigsByGroupId.forEach((groupId, list) -> {
            TaskCfg selectedTask = selectTaskFromGroup(list, currentDate);
            if (selectedTask != null) {
                taskCfgList.add(selectedTask);
            }
        });

        return taskCfgList;
    }

    /**
     * 从任务组中选择合适的任务
     * 优先选择时间配置与当前年月相同的任务，如果没有则选择没有时间配置的常驻任务
     *
     * @param taskList    任务组列表
     * @param currentDate 当前日期
     * @return 选中的任务配置，如果没有合适的任务则返回null
     */
    private TaskCfg selectTaskFromGroup(List<TaskCfg> taskList, LocalDate currentDate) {
        // 筛选出有时间配置且与当前年月匹配的任务
        List<TaskCfg> currentMonthTasks = taskList.stream()
                .filter(cfg -> isTaskMatchCurrentMonth(cfg, currentDate))
                .toList();

        //只有一个就直接返回
        if (currentMonthTasks.size() == 1) {
            return currentMonthTasks.getFirst();
        }

        // 如果有匹配当前年月的任务，选择ID最小的
        if (!currentMonthTasks.isEmpty()) {
            return currentMonthTasks.stream()
                    .min(Comparator.comparingInt(TaskCfg::getId))
                    .orElse(null);
        }

        // 如果没有匹配当前年月的任务，选择没有时间配置的常驻任务
        List<TaskCfg> permanentTasks = taskList.stream()
                .filter(cfg -> cfg.getTime() == null || cfg.getTime().isEmpty())
                .toList();

        //只有一个就直接返回
        if (permanentTasks.size() == 1) {
            return permanentTasks.getFirst();
        }

        if (!permanentTasks.isEmpty()) {
            return permanentTasks.stream()
                    .min(Comparator.comparingInt(TaskCfg::getId))
                    .orElse(null);
        }

        return null;
    }

    /**
     * 检查任务是否匹配当前年月
     *
     * @param taskCfg     任务配置
     * @param currentDate 当前日期
     * @return true 如果任务时间配置与当前年月匹配
     */
    private boolean isTaskMatchCurrentMonth(TaskCfg taskCfg, LocalDate currentDate) {
        String timeStr = taskCfg.getTime();
        if (timeStr == null || timeStr.isEmpty()) {
            return false;
        }

        long timestamp = TimeHelper.getTimestamp(timeStr.trim());
        if (timestamp <= TaskConstant.TimeConstants.INVALID_TIMESTAMP_THRESHOLD) {
            return false;
        }

        LocalDate taskDate = getLocalDateFromTimestamp(timestamp);
        // 检查年月是否相同
        return taskDate.getYear() == currentDate.getYear() &&
                taskDate.getMonth() == currentDate.getMonth();
    }

    /**
     * 为玩家创建新任务
     *
     * @param playerId             玩家ID
     * @param playerTasks          玩家任务Map
     * @param availableTaskConfigs 可用任务配置列表
     * @return 新创建的任务列表
     */
    private List<TaskData> createNewTasksForPlayer(long playerId, RMap<Integer, TaskData> playerTasks, List<TaskCfg> availableTaskConfigs) {
        List<TaskData> receiveList = new ArrayList<>();
        //检测是否有新任务
        if (!availableTaskConfigs.isEmpty()) {
            availableTaskConfigs.forEach(taskCfg -> {
                List<Integer> taskCfgTaskConditionId = taskCfg.getTaskConditionId();
                if (taskCfgTaskConditionId != null && !taskCfgTaskConditionId.isEmpty()) {
                    int conditionId = taskCfgTaskConditionId.getFirst();
                    AbstractTaskCondition<DefaultTaskConditionParam> abstractTaskCondition = taskManager.getTaskCondition(conditionId);
                    //有处理器才给玩家领取任务
                    if (abstractTaskCondition != null) {
                        //不重复领取任务
                        if (!playerTasks.containsKey(taskCfg.getId())) {
                            TaskData taskData = createNewTaskData(playerId, taskCfg.getId());
                            receiveList.add(taskData);
                        }
                    } else {
                        log.warn("配置任务[{}],条件[{}]没有处理器!", taskCfg.getId(), conditionId);
                    }
                }
            });
        }

        return receiveList;
    }

    /**
     * 创建新的任务数据
     *
     * @param playerId 玩家ID
     * @param taskId   任务配置ID
     * @return 新的任务数据
     */
    private TaskData createNewTaskData(long playerId, int taskId) {
        TaskData taskData = new TaskData();
        taskData.setConfigId(taskId);
        taskData.setStatus(TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        taskData.setCreateTime(System.currentTimeMillis());
        taskData.setPlayerId(playerId);
        return taskData;
    }

    /**
     * 检测任务是否激活 没有时间限制默认激活
     *
     * @param taskCfg 任务配置
     * @return true 激活
     */
    public boolean checkTaskActive(TaskCfg taskCfg) {
        return isTaskTimeActive(taskCfg.getTime());
    }

    /**
     * 检查任务时间是否激活
     *
     * @param timeStr 时间字符串
     * @return true 激活
     */
    private boolean isTaskTimeActive(String timeStr) {
        //没有时间限制
        if (timeStr == null || timeStr.isEmpty()) {
            return true;
        }
        long timestamp = TimeHelper.getTimestamp(timeStr.trim());
        //没有时间限制
        if (timestamp <= TaskConstant.TimeConstants.INVALID_TIMESTAMP_THRESHOLD) {
            return true;
        }
        LocalDate taskDate = getLocalDateFromTimestamp(timestamp);
        LocalDate currentDate = LocalDate.now();
        return taskDate.isEqual(currentDate) || taskDate.isBefore(currentDate);
    }

    /**
     * 从时间戳获取LocalDate
     *
     * @param timestamp 时间戳
     * @return LocalDate
     */
    private LocalDate getLocalDateFromTimestamp(long timestamp) {
        return LocalDate.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * 从时间戳获取LocalDateTime
     *
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    private LocalDateTime getLocalDateTimeFromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * 是否需要删除任务
     *
     * @return true 需要删除
     */
    public boolean shouldDelete(TaskData taskData, TaskCfg taskCfg) {
        // 获取同组的激活任务
        List<TaskCfg> activeGroupTasks = getActiveTasksInGroup(taskCfg.getGroup());

        // 任务已经过期（不在激活任务列表中）
        if (activeGroupTasks.stream().noneMatch(cfg -> cfg.getId() == taskCfg.getId())) {
            return true;
        }

        // 获取当前应该激活的任务
        TaskCfg currentActiveCfg = getCurrentActiveTaskInGroup(activeGroupTasks);

        // 如果当前任务不是应该激活的任务，则删除
        if (currentActiveCfg.getId() != taskCfg.getId()) {
            return true;
        }

        // 积分大奖任务需要检查时间刷新
        if (taskCfg.getTaskType() == TaskConstant.TaskType.POINTS_AWARD) {
            LocalDateTime createTime = getLocalDateTimeFromTimestamp(taskData.getCreateTime());
            return shouldRefreshTask(createTime);
        }

        return false;
    }

    /**
     * 获取指定组中的激活任务列表
     *
     * @param groupId 组ID
     * @return 激活任务列表
     */
    private List<TaskCfg> getActiveTasksInGroup(int groupId) {
        return GameDataManager.getTaskCfgList().stream()
                .filter(cfg -> cfg.getGroup() == groupId && checkTaskActive(cfg))
                .toList();
    }

    /**
     * 获取组中当前应该激活的任务
     *
     * @param activeGroupTasks 组中的激活任务列表
     * @return 当前应该激活的任务
     */
    private TaskCfg getCurrentActiveTaskInGroup(List<TaskCfg> activeGroupTasks) {
        LocalDate currentDate = LocalDate.now();

        // 筛选出与当前年月匹配的任务
        List<TaskCfg> currentMonthTasks = activeGroupTasks.stream()
                .filter(cfg -> isTaskMatchCurrentMonth(cfg, currentDate))
                .toList();

        if (currentMonthTasks.size() == 1) {
            return currentMonthTasks.getFirst();
        }

        // 优先选择与当前年月匹配的任务，有多个则选择ID最小的
        if (!currentMonthTasks.isEmpty()) {
            return currentMonthTasks.stream()
                    .min(Comparator.comparingInt(TaskCfg::getId))
                    .orElse(currentMonthTasks.getFirst());
        }

        // 如果没有匹配当前年月的任务，选择没有时间配置的常驻任务中ID最小的
        List<TaskCfg> permanentTasks = activeGroupTasks.stream()
                .filter(cfg -> cfg.getTime() == null || cfg.getTime().isEmpty())
                .toList();

        if (permanentTasks.size() == 1) {
            return permanentTasks.getFirst();
        }

        if (!permanentTasks.isEmpty()) {
            return permanentTasks.stream()
                    .min(Comparator.comparingInt(TaskCfg::getId))
                    .orElse(permanentTasks.getFirst());
        }

        // 如果都没有，返回ID最小的任务作为兜底
        return activeGroupTasks.stream()
                .min(Comparator.comparingInt(TaskCfg::getId))
                .orElse(activeGroupTasks.getFirst());
    }

    /**
     * 根据配置筛选是否需要玩家领取任务
     *
     * @param taskCfg 任务配置
     * @return
     */
    public boolean filterReceiveTask(TaskCfg taskCfg) {
        //积分任务有时间限制
        if (taskCfg.getTaskType() == TaskConstant.TaskType.POINTS_AWARD) {
            return checkTaskActive(taskCfg);
        } else {
            return true;
        }
    }

    /**
     * 组装任务条件消息体
     *
     * @param taskData 任务数据
     * @param taskCfg  任务配置
     * @return 任务条件消息体
     */
    public List<TaskCondition> assembleTaskConditions(TaskData taskData, TaskCfg taskCfg) {
        List<TaskCondition> conditions = new ArrayList<>();
        Map<Integer, Long> taskDataProgress = taskData.getProgress();
        // 判断进度是否为空，统一处理
        if (taskDataProgress == null || taskDataProgress.isEmpty()) {
            addTaskConditionToList(taskCfg.getTaskConditionId().getFirst(), taskData, taskCfg, conditions);
        } else {
            taskDataProgress.forEach((taskId, progress) -> addTaskConditionToList(taskId, taskData, taskCfg, conditions));
        }
        return conditions;
    }

    private void addTaskConditionToList(int conditionId, TaskData taskData, TaskCfg taskCfg, List<TaskCondition> conditions) {
        try {
            AbstractTaskCondition<DefaultTaskConditionParam> taskCondition = taskManager.getTaskCondition(conditionId);
            if (taskCondition != null) {
                TaskCondition condition = taskCondition.assembleTaskCondition(taskData, taskCfg);
                conditions.add(condition);
            } else {
                log.warn("任务[{}]条件[{}]找不到条件处理器! playerId={}",
                        taskData.getConfigId(), conditionId, taskData.getPlayerId());
            }
        } catch (Exception e) {
            log.error("组装任务条件失败 taskId={}, conditionId={}, playerId={}, error={}",
                    taskData.getConfigId(), conditionId, taskData.getPlayerId(), e.getMessage(), e);
        }
    }

    /**
     * 组装任务消息
     *
     * @param taskData 任务数据
     * @param config   配置
     * @return 任务消息体
     */
    public Task assembleTask(TaskData taskData, TaskCfg config) {
        Task task = new Task();
        task.setConfigId(config.getId());
        //任务状态
        task.setStatus(taskData.getStatus());
        //任务条件
        task.setConditions(assembleTaskConditions(taskData, config));
        return task;
    }

    /**
     * 通知玩家任务更新
     */
    public void noticeUpdate(long playerId, TaskData taskData, TaskCfg config) {
        Task task = assembleTask(taskData, config);
        NotifyUpdateTask updateTask = new NotifyUpdateTask();
        updateTask.setTaskList(List.of(task));
        PFSession pfSession = clusterSystem.getSession(playerId);
        if (pfSession != null) {
            pfSession.send(updateTask);
        }
    }

    /**
     * 通知玩家任务更新
     */
    public void noticeUpdateAll(long playerId) {
        NotifyUpdateTask updateTask = new NotifyUpdateTask();
        RMap<Integer, TaskData> dataMap = getPlayerTaskMap(playerId);
        if (dataMap != null) {
            //已领取任务不显示
            List<Task> tasks = dataMap.values().stream().map(taskData -> {
                if (taskData.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED) {
                    return null;
                }
                TaskCfg taskCfg = GameDataManager.getTaskCfg(taskData.getConfigId());
                if (taskCfg == null) {
                    return null;
                }
                return assembleTask(taskData, taskCfg);
            }).filter(Objects::nonNull).toList();
            //有任务才推送更新
            if (!tasks.isEmpty()) {
                updateTask.setTaskList(tasks);
                PFSession pfSession = clusterSystem.getSession(playerId);
                if (pfSession != null) {
                    pfSession.send(updateTask);
                }
            }
        }
    }

    /**
     * 获取所属模块{@link RedDotDetails.RedDotModule}
     */
    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.TASK;
    }

    /**
     * 初始化红点信息
     *
     * @param playerId  玩家id
     * @param submodule 子模块
     *                  </p>
     *                  (如果指定了子模块则加载子模块数据,没有则加载所有子模块)
     */
    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        List<RedDotDetails> redDotList = new ArrayList<>();
        RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
        Collection<TaskData> tasks = playerTasks.values();
        if (!tasks.isEmpty()) {
            List<TaskData> pointAwardTasks;
            if (submodule == 0) {
                pointAwardTasks = tasks.stream().toList();
            } else {
                //筛选任务
                pointAwardTasks = tasks.stream()
                        .filter(taskData -> {
                            TaskCfg taskCfg = GameDataManager.getTaskCfg(taskData.getConfigId());
                            return taskCfg.getTaskType() == submodule;
                        })
                        .toList();
            }
            if (!pointAwardTasks.isEmpty()) {
                RedDotDetails redDotDetails = new RedDotDetails();
                redDotDetails.setRedDotType(RedDotDetails.RedDotType.COUNT);
                redDotDetails.setRedDotModule(RedDotDetails.RedDotModule.TASK);
                //红点子模块
                redDotDetails.setRedDotSubmodule(submodule);
                List<Integer> taskIds = tasks.stream().map(TaskData::getConfigId).toList();
                redDotDetails.setCount(taskIds.size());
                JSONObject extra = new JSONObject();
                extra.put("taskIds", taskIds);
                redDotDetails.setExtra(extra.toJSONString());
                redDotList.add(redDotDetails);
            }
        }
        return redDotList;
    }

    /**
     * 获取玩家任务列表
     *
     * @param playerId 任务列表
     * @return
     */
    public List<Task> getPlayerTaskList(long playerId, int type) {
        List<Task> taskList = new ArrayList<>();
        RMap<Integer, TaskData> dataMap = getPlayerTaskMap(playerId);
        if (dataMap == null) {
            return taskList;
        }
        //已领取任务不显示
        return dataMap.values().stream().map(taskData -> {
            if (taskData.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED) {
                return null;
            }
            TaskCfg taskCfg = GameDataManager.getTaskCfg(taskData.getConfigId());
            if (taskCfg == null) {
                return null;
            }
            if (taskCfg.getTaskType() != type) {
                return null;
            }
            return assembleTask(taskData, taskCfg);
        }).filter(Objects::nonNull).toList();
    }

    /**
     * 领取任务奖励
     *
     * @param playerId 玩家id
     * @param taskId   任务配置id
     * @return true 领取成功
     */
    public boolean receiveTask(long playerId, int taskId) {
        TaskCfg taskCfg = GameDataManager.getTaskCfg(taskId);
        if (taskCfg == null) {
            log.warn("玩家[{}]领取[{}]任务奖励失败!配置不存在!", playerId, taskId);
            return false;
        }
        RMap<Integer, TaskData> taskDataMap = getPlayerTaskMap(playerId);
        if (taskDataMap == null) {
            log.error("玩家[{}]任务数据Map为空", playerId);
            return false;
        }

        try {
            return redisLock.lockAndGet(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
                TaskData taskData = taskDataMap.get(taskId);
                if (taskData == null) {
                    log.warn("玩家[{}]领取[{}]任务奖励失败!任务不存在!", playerId, taskId);
                    return false;
                }
                //状态不对
                if (taskData.getStatus() != TaskConstant.TaskStatus.STATUS_COMPLETED) {
                    log.warn("玩家[{}]领取[{}]任务奖励失败!任务状态[{}]", playerId, taskId, taskData.getStatus());
                    return false;
                }

                try {
                    //修改任务状态
                    taskData.setRewardTime(System.currentTimeMillis());
                    taskData.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
                    //同步到缓存中
                    taskDataMap.fastPut(taskData.getConfigId(), taskData);
                    //同步到数据库
                    taskDataDao.save(taskData);

                    List<Integer> getItem = taskCfg.getGetItem();
                    int integralNum = taskCfg.getIntegralNum();
                    //如果有积分奖励通知大厅
                    if (integralNum > TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD) {
                        addPlayerPoints(playerId, integralNum, true);
                    }
                    List<Item> itemList = new ArrayList<>();
                    if (!getItem.isEmpty()) {
                        itemList = ItemUtils.buildItems(getItem);
                        playerPackService.addItems(playerId, itemList, "taskAward");
                    }
                    //记录日志
                    taskLogger.receiveTaskAward(playerId, taskId, itemList, integralNum);
                    redDotManager.updateRedDot(this, taskCfg.getTaskType(), playerId);

                    log.info("玩家[{}]成功领取任务[{}]奖励", playerId, taskId);
                    return true;
                } catch (Exception e) {
                    log.error("领取任务奖励过程中发生异常 playerId={}, taskId={}, error={}",
                            playerId, taskId, e.getMessage(), e);
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("领取任务奖励失败 playerId={}, taskId={}, error={}", playerId, taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 增加玩家积分
     *
     * @param playerId 玩家id
     * @param value    变化值
     * @param flag     变化 true增加 false扣除
     */
    public void addPlayerPoints(long playerId, int value, boolean flag) {
        if (flag) {
            hallPointsAwardBridge.add(playerId, value, PointsAwardType.TASK);
        } else {
            hallPointsAwardBridge.deduct(playerId, value, PointsAwardType.TASK);
        }
    }

    /**
     * 检查所有在线玩家的任务
     * 在关键时间点（0点、12点）触发全局任务检查
     *
     * @param hour 触发的小时
     */
    private void checkAllOnlinePlayerTasks(int hour) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                // 获取所有在线玩家ID
                Set<Long> onlinePlayerIds = clusterSystem.getAllOnlinePlayerId();
                log.info("时钟事件[{}点]触发，开始检查{}个在线玩家的任务", hour, onlinePlayerIds.size());
                int failCount = 0;
                for (Long playerId : onlinePlayerIds) {
                    try {
                        executor.submit(() -> {
                            // 使用非阻塞方式检查任务，避免影响其他玩家
                            checkTask(playerId);
                            //重新通知客户端刷新一次列表 推送玩家所有任务更新
                            noticeUpdateAll(playerId);
                        });
                    } catch (Exception e) {
                        failCount++;
                        log.error("时钟事件检查玩家[{}]任务失败: {}", playerId, e.getMessage(), e);
                    }
                }
                log.info("时钟事件[{}点]任务检查完成，total[{}] 失败: {}", hour, onlinePlayerIds.size(), failCount);
            } catch (Exception e) {
                log.error("时钟事件[{}点]获取在线玩家列表失败: {}", hour, e.getMessage(), e);
            }
        }

    }

    /**
     * 处理事件
     *
     * @param gameEvent 事件
     */
    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent clockEvent) {
            int hour = clockEvent.getHour();
            //处理日常任务
            if (hour == 0) {
                checkAllOnlinePlayerTasks(hour);
            }
            //积分大奖任务额外处理
            else if (hour == 12) {
                checkAllOnlinePlayerTasks(hour);
            }
        }
    }

    /**
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT);
    }
}