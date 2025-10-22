package com.jjg.game.core.task.service;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.NotifyPointsUpdate;
import com.jjg.game.core.pb.reddot.RedDotDetails;
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
        //同步到数据库
        taskDataDao.save(taskData);
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
            //接了任务才触发
            if (taskData != null) {
                TaskData data = redisLock.lockAndGet(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
                    TaskData tempData = taskDataDao.findByPlayerIdAndConfigId(playerId, taskCfg.getId());
                    if (tempData == null) {
                        return null;
                    }
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
                            if (awardList.isEmpty() && taskCfg.getIntegralNum() <= 0) {
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
                List<Integer> taskCfgTaskConditionId = taskCfg.getTaskConditionId();
                if (taskCfgTaskConditionId != null && !taskCfgTaskConditionId.isEmpty()) {
                    int conditionId = taskCfgTaskConditionId.getFirst();
                    AbstractTaskCondition<DefaultTaskConditionParam> abstractTaskCondition = taskManager.getTaskCondition(conditionId);
                    //有处理器才给玩家领取任务
                    if (abstractTaskCondition != null) {
                        //不重复领取任务
                        if (!playerTasks.containsKey(taskCfg.getId())) {
                            TaskData taskData = new TaskData();
                            taskData.setConfigId(taskCfg.getId());
                            taskData.setStatus(TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
                            taskData.setCreateTime(System.currentTimeMillis());
                            taskData.setPlayerId(playerId);
                            receiveList.add(taskData);
                        }
                    } else {
                        log.warn("配置任务[{}],条件[{}]没有处理器!", taskCfg.getId(), conditionId);
                    }
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
            redDotManager.updateRedDot(this, 0, playerId);
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
        AbstractTaskCondition<DefaultTaskConditionParam> taskCondition = taskManager.getTaskCondition(conditionId);
        if (taskCondition != null) {
            TaskCondition condition = taskCondition.assembleTaskCondition(taskData, taskCfg);
            conditions.add(condition);
        } else {
            log.warn("任务[{}]条件[{}]找不到条件处理器!", taskData.getConfigId(), conditionId);
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
        return redisLock.lockAndGet(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
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
            if (integralNum > 0) {
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
            return true;
        });
    }

    /**
     * 增加玩家积分
     *
     * @param playerId 玩家id
     * @param value    变化值
     * @param flag     变化 true增加 false扣除
     */
    public void addPlayerPoints(long playerId, int value, boolean flag) {
        NotifyPointsUpdate notifyPointsUpdate = new NotifyPointsUpdate();
        notifyPointsUpdate.setFlag(flag);
        notifyPointsUpdate.setPlayerId(playerId);
        notifyPointsUpdate.setValue(value);
        notifyPointsUpdate.setType(PointsAwardType.TASK);
        clusterSystem.notifyNode(MessageUtil.getPFMessage(notifyPointsUpdate), Set.of(NodeType.HALL.toString())::contains);
    }

}