package com.jjg.game.core.task.service;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDataDao;
import com.jjg.game.core.task.condition.AbstractTaskCondition;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.core.task.pb.TaskJump;
import com.jjg.game.core.task.pb.TaskReward;
import com.jjg.game.core.task.pb.res.NotifyUpdateTask;
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

/**
 * 任务服务
 * 负责玩家任务的初始化、进度更新、完成检查和奖励发放
 */
@Service
public class TaskService implements IRedDotService, IPlayerLoginSuccess {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final ClusterSystem clusterSystem;
    private final TaskLogger taskLogger;
    private final RedDotManager redDotManager;
    private final RedissonClient redissonClient;
    private final TaskDataDao taskDataDao;
    private final RedisLock redisLock;
    private final PlayerPackService playerPackService;

    /**
     * 锁时间。
     */
    private static final int LOCK_TIME = 2000;

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
            log.error("初始化玩家任务失败 playerId={}", playerId, e);
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
        RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
        redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
            //同步到缓存中
            playerTasks.fastPut(taskData.getConfigId(), taskData);
        });
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
            taskData = playerTasks.get(taskCfg.getId());
            if (taskData == null) {
                taskData = taskDataDao.findByPlayerIdAndConfigId(playerId, taskCfg.getId());
            }
            //接了任务才触发
            if (taskData != null) {
                boolean isTriggered = condition.trigger(taskCfg, taskData, param);
                log.info("玩家[{}]触发任务[{}]条件[{}]触发[{}]", playerId, taskCfg.getId(), condition.getClass().getSimpleName(), isTriggered);
                if (isTriggered) {
                    LocalDateTime now = LocalDateTime.now();
                    long timestamp = TimeHelper.getTimestamp(now);
                    //任务所有条件都完成了
                    if (taskData.getFinishConditionIds().size() == taskData.getProgress().size()) {
                        taskData.setCompleteTime(timestamp);
                        //检测是否有奖励
                        List<Integer> awardList = taskCfg.getGetItem();
                        //没有奖励的任务 默认直接完成并且已经领取奖励
                        if (awardList.isEmpty()) {
                            taskData.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
                            taskData.setRewardTime(timestamp);
                            taskLogger.receiveTaskAward(playerId, taskData.getConfigId(), null);
                            log.info("玩家[{}]完成任务[{}]任务没有奖励直接修改为已领取状态!", playerId, taskData.getConfigId());
                        } else {
                            taskData.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
                            taskLogger.completeTask(playerId, taskData.getConfigId());
                            log.info("玩家[{}]完成任务[{}]", playerId, taskData.getConfigId());
                        }
                    }
                    updatePlayerTaskCache(playerId, taskData);
                    //更新到数据库中
                    taskDataDao.save(taskData);
                    //通知进度更新
                    noticeUpdate(playerId, taskData, taskCfg);
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
        RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
        List<TaskData> tasks;
        tasks = playerTasks.values().stream().toList();
        if (tasks.isEmpty()) {
            tasks = taskDataDao.findByPlayerId(playerId);
            if (tasks != null) {
                for (TaskData taskData : tasks) {
                    playerTasks.fastPut(taskData.getConfigId(), taskData);
                }
            }
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
        return createHour < 12 && nowHour >= 12;
    }

    /**
     * 检测玩家任务
     */
    public void checkTask(long playerId) {
        RMap<Integer, TaskData> playerTasks = getPlayerTaskMap(playerId);
        boolean noticeRedDot = false;
        List<TaskData> deletedTasks = new ArrayList<>();
        for (Map.Entry<Integer, TaskData> entry : playerTasks.entrySet()) {
            Integer taskId = entry.getKey();
            TaskData taskData = entry.getValue();
            TaskCfg taskCfg = GameDataManager.getTaskCfg(taskId);
            boolean delete = false;
            //有配置才处理
            if (taskCfg != null) {
                //积分大奖任务处理
                if (taskCfg.getTaskType() == TaskConstant.TaskType.POINTS_AWARD) {
                    long timestamp = taskData.getCreateTime();
                    LocalDateTime createTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                    );
                    //任务过期
                    if (shouldRefreshTask(createTime)) {
                        delete = true;
                    }
                }
            }
            //配置不存在则删除任务
            else {
                delete = true;
            }
            if (delete) {
                deletedTasks.add(taskData);
            }
        }
        //有需要删除的过期任务
        if (!deletedTasks.isEmpty()) {
            //删除数据库
            taskDataDao.deleteAllList(deletedTasks);
            redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
                for (TaskData taskData : deletedTasks) {
                    playerTasks.fastRemove(taskData.getConfigId());
                    log.info("玩家[{}]任务[{}]过期!", playerId, taskData.getConfigId());
                }
            });
            noticeRedDot = true;
        }
        //根据配置检测是否需要领取新任务
        List<TaskCfg> taskCfgList = GameDataManager.getTaskCfgList().stream()
                .filter(t -> {
                    //积分任务有时间限制
                    if (t.getTaskType() == TaskConstant.TaskType.POINTS_AWARD) {
                        String time = t.getTime();
                        //没有时间限制
                        if (time == null || time.isEmpty()) {
                            return true;
                        }
                        long timestamp = TimeHelper.getTimestamp(time.trim());
                        //没有时间限制
                        if (timestamp <= 0) {
                            return true;
                        }
                        LocalDate dateTime = LocalDate.ofInstant(
                                Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault()
                        );
                        return dateTime.isEqual(LocalDate.now());
                    } else {
                        return true;
                    }
                })
                .toList();
        List<TaskData> receiveList = new ArrayList<>();
        //检测是否有新任务
        if (!taskCfgList.isEmpty()) {
            taskCfgList.forEach(taskCfg -> {
                //不重复领取任务
                if (!playerTasks.containsKey(taskCfg.getId())) {
                    TaskData taskData = new TaskData();
                    taskData.setConfigId(taskCfg.getId());
                    taskData.setStatus(TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
                    taskData.setCreateTime(System.currentTimeMillis());
                    taskData.setPlayerId(playerId);
                    receiveList.add(taskData);
                }
            });
        }
        //有任务才更新
        if (!receiveList.isEmpty()) {
            //覆盖数据库数据
            taskDataDao.saveAll(receiveList);
            //覆盖缓存
            redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () ->
                    receiveList.forEach(taskData -> {
                        playerTasks.fastPut(taskData.getConfigId(), taskData);
                        //记录领取任务日志
                        taskLogger.receiveTask(playerId, taskData.getConfigId());
                        log.info("玩家[{}]领取到[{}]任务", playerId, taskData.getConfigId());
                    })
            );
            noticeRedDot = true;
        }
        if (noticeRedDot) {
            //更新红点信息
            updateRedDot(playerId, 0);
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
        //任务进度
        Map<Integer, Long> taskDataProgress = taskData.getProgress();
        taskDataProgress.forEach((taskId, progress) -> {
            AbstractTaskCondition<DefaultTaskConditionParam> taskCondition = taskManager.getTaskCondition(taskId);
            TaskCondition condition = taskCondition.assembleTaskCondition(taskData, taskCfg);
            conditions.add(condition);
        });
        return conditions;
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
        //任务奖励
        task.setRewards(taskRewards(config));
        //任务跳转类型
        task.setJumps(taskJump(config));
        //任务状态
        task.setStatus(taskData.getStatus());
        //图标
        task.setTaskIcon(config.getTaskIcon());
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
     * 任务奖励
     *
     * @param taskCfg 任务配置
     * @return 任务奖励消息体
     */
    private List<TaskReward> taskRewards(TaskCfg taskCfg) {
        List<TaskReward> rewards = new ArrayList<>();
        List<Integer> getItem = taskCfg.getGetItem();
        if (getItem != null && !getItem.isEmpty()) {
            for (int i = 0; i < getItem.size(); i += 2) {
                TaskReward reward = new TaskReward();
                reward.setItemId(getItem.get(i));
                reward.setItemNum(getItem.get(i + 1));
                rewards.add(reward);
            }
        }
        return rewards;
    }

    /**
     * 任务跳转
     *
     * @param taskCfg 任务配置
     * @return 任务跳转消息体
     */
    private List<TaskJump> taskJump(TaskCfg taskCfg) {
        List<TaskJump> jumps = new ArrayList<>();
        List<Integer> jumpType = taskCfg.getJumpType();
        if (jumpType != null && !jumpType.isEmpty()) {
            for (int i = 0; i < jumpType.size(); i += 2) {
                TaskJump jump = new TaskJump();
                jump.setType(jumpType.get(i));
                jump.setValue(jumpType.get(i + 1));
                jumps.add(jump);
            }
        }
        return jumps;
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
            //更新积分大奖任务
            if (submodule == TaskConstant.RedDotModule.POINTS_AWARD) {
                RedDotDetails details = pointsAward(tasks);
                if (details != null) {
                    redDotList.add(details);
                }
            }
            //所有任务系统红点
            else {
                RedDotDetails details = pointsAward(tasks);
                if (details != null) {
                    redDotList.add(details);
                }
            }
        }
        return redDotList;
    }

    /**
     * 积分大奖红点信息
     *
     * @param tasks 所有任务
     */
    public RedDotDetails pointsAward(Collection<TaskData> tasks) {
        //积分大奖任务
        List<TaskData> pointAwardTasks = tasks.stream()
                .filter(taskData -> {
                    TaskCfg taskCfg = GameDataManager.getTaskCfg(taskData.getConfigId());
                    return taskCfg.getTaskType() == TaskConstant.TaskType.POINTS_AWARD;
                })
                .toList();
        if (!pointAwardTasks.isEmpty()) {
            RedDotDetails redDotDetails = new RedDotDetails();
            redDotDetails.setRedDotType(RedDotDetails.RedDotType.COUNT);
            redDotDetails.setRedDotModule(RedDotDetails.RedDotModule.TASK);
            //红点子模块
            redDotDetails.setRedDotSubmodule(TaskConstant.RedDotModule.POINTS_AWARD);
            List<Integer> taskIds = tasks.stream().map(TaskData::getConfigId).toList();
            redDotDetails.setCount(taskIds.size());
            JSONObject extra = new JSONObject();
            extra.put("taskIds", taskIds);
            redDotDetails.setExtra(extra.toJSONString());
            return redDotDetails;
        }
        return null;
    }

    /**
     * 更新任务红点信息
     *
     * @param playerId 玩家id
     */
    public void updateRedDot(long playerId, int submodule) {
        List<RedDotDetails> redDotDetails;
        if (submodule > 0) {
            redDotDetails = initialize(playerId, submodule);
        } else {
            redDotDetails = initialize(playerId, 0);
        }
        redDotManager.updateRedDot(redDotDetails, playerId);
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
        return dataMap.values().stream().map(taskData -> {
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
            log.info("玩家[{}]领取[{}]任务奖励失败!配置不存在!", playerId, taskId);
            return false;
        }
        RMap<Integer, TaskData> taskDataMap = getPlayerTaskMap(playerId);
        if (taskDataMap == null) {
            return false;
        }
        TaskData taskData = taskDataMap.get(taskId);
        if (taskData == null) {
            log.info("玩家[{}]领取[{}]任务奖励失败!任务不存在!", playerId, taskId);
            return false;
        }
        //状态不对
        if (taskData.getStatus() != TaskConstant.TaskStatus.STATUS_COMPLETED) {
            log.info("玩家[{}]领取[{}]任务奖励失败!任务状态[{}]", playerId, taskId, taskData.getStatus());
            return false;
        }
        List<Integer> getItem = taskCfg.getGetItem();
        if (!getItem.isEmpty()) {
            taskData.setRewardTime(System.currentTimeMillis());
            taskData.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
            Map<Integer, Long> item = new HashMap<>();
            for (int i = 0; i < getItem.size(); i += 2) {
                item.put(getItem.get(i), Long.valueOf(getItem.get(i + 1)));
            }
            playerPackService.addItems(playerId, item, "taskAward");
            //记录日志
            taskLogger.receiveTaskAward(playerId, taskId, item);
        }
        updateRedDot(playerId, taskCfg.getTaskType());
        return true;
    }

}