package com.jjg.game.core.task.service;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.PointsAwardType;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.rpc.HallPointsAwardBridge;
import com.jjg.game.core.task.condition.AbstractTaskCondition;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDataDao;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务服务
 * 负责玩家任务的初始化、进度更新、完成检查和奖励发放
 */
@Service
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final String TABLE_NAME = "task:player";

    private final TaskLogger taskLogger;
    private final RedissonClient redissonClient;
    private final TaskDataDao taskDataDao;
    private final RedisLock redisLock;
    @ClusterRpcReference()
    private HallPointsAwardBridge hallPointsAwardBridge;

    /**
     * 锁时间。
     */
    private static final int LOCK_TIME = 10000;


    public TaskService(RedissonClient redissonClient,
                       TaskDataDao taskDataDao,
                       RedisLock redisLock,
                       TaskLogger taskLogger) {
        this.redissonClient = redissonClient;
        this.taskDataDao = taskDataDao;
        this.redisLock = redisLock;
        this.taskLogger = taskLogger;
    }


    /**
     * 获取玩家的任务Map
     *
     * @param playerId 玩家ID
     * @return 任务数据Map
     */
    public TaskData getPlayerTask(long playerId) {
        RMap<Long, TaskData> map = getPlayerTaskMap();
        TaskData taskData = map.get(playerId);
        if (taskData == null) {
            TaskData data = taskDataDao.findByPlayerId(playerId);
            if (data == null) {
                data = new TaskData();
                data.setPlayerId(playerId);
            }
            redissonClient.getMap(TABLE_NAME).fastPut(playerId, data);
            return data;
        }
        return taskData;
    }

    public RMap<Long, TaskData> getPlayerTaskMap() {
        return redissonClient.getMap(TABLE_NAME);
    }


    /**
     * 检测玩家任务
     */
    public void checkTask(long playerId, TaskData taskData, TaskManager taskManager) {
        removeExpiredTasks(playerId, taskData, taskManager);
        if (addNewTasks(playerId, taskData, taskManager)) {
            taskManager.updateRedDot(playerId);
        }
    }

    /**
     * 回存任务
     * @param playerId 玩家id
     * @param taskData 任务数据
     */
    public void saveTask(long playerId, TaskData taskData) {
        redissonClient.getMap(TABLE_NAME).fastPut(playerId, taskData);
    }

    /**
     * 删除过期任务
     *
     * @param playerId    玩家ID
     * @param taskManager 任务管理器
     */
    private void removeExpiredTasks(long playerId, TaskData taskData, TaskManager taskManager) {
        try {
            if (taskData == null || taskData.getTaskDetails() == null || taskData.getTaskDetails().isEmpty()) {
                return;
            }

            taskData.getTaskDetails().entrySet().removeIf(en -> {
                Integer taskId = en.getKey();
                TaskDetail detail = en.getValue();
                TaskCfg taskCfg = GameDataManager.getTaskCfg(taskId);

                //有配置才处理
                if (taskCfg != null) {
                    //检测任务是否需要删除
                    return shouldDelete(detail, taskCfg, taskManager);
                } else {
                    //配置不存在则删除任务
                    log.warn("任务配置不存在，将删除任务 playerId={}, taskId={}", playerId, taskId);
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("删除过期任务失败 playerId={}, error={}", playerId, e.getMessage(), e);
        }

    }

    /**
     * 是否需要删除任务
     *
     * @return true 需要删除
     */
    public boolean shouldDelete(TaskDetail taskDetail, TaskCfg taskCfg, TaskManager taskManager) {
        List<TaskCfg> taskGroupCfgs = taskManager.getTaskCfgMap().get(taskCfg.getGroup());
        if (CollectionUtil.isEmpty(taskGroupCfgs)) {
            return true;
        }
        // 获取同组的激活任务
        List<TaskCfg> activeGroupTasks = taskGroupCfgs.stream().filter(cfg -> checkTaskActive(taskCfg)).toList();
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
            LocalDateTime createTime = getLocalDateTimeFromTimestamp(taskDetail.getCreateTime());
            return shouldRefreshTask(createTime);
        }

        return false;
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
     * 触发玩家任务
     *
     * @param playerId    玩家id
     * @param taskCfgList 需要触发的任务配置列表
     * @param param       参数
     * @return true 需要红点
     */
    public boolean trigger(long playerId, TaskData taskData, List<TaskCfg> taskCfgList, AbstractTaskCondition<DefaultTaskConditionParam> condition,
                           DefaultTaskConditionParam param, boolean isNotify) {
//        log.info("玩家[{}]触发条件[{}]conditionId[{}]参数[{}]", playerId, condition.getClass().getSimpleName(), condition.getId(), param == null ? "null" : param.toString());
        List<Pair<TaskDetail, TaskCfg>> updateTasks = new ArrayList<>();
        AtomicBoolean hasFinished = new AtomicBoolean(false);
        try {
            long timestamp = System.currentTimeMillis();
            for (TaskCfg taskCfg : taskCfgList) {
                //接了任务才触发
                TaskDetail taskDetail = taskData.getTaskDetail(taskCfg.getId());
                if (taskDetail == null) {
                    continue;
                }
                //从内存加载最新的任务对象进行处理
                boolean isTriggered = condition.trigger(playerId, taskCfg, taskDetail, param);
                if (isTriggered) {
                    //任务所有条件都完成了
                    if (taskDetail.getFinishConditionIds().size() == taskDetail.getProgress().size()) {
                        taskDetail.setCompleteTime(timestamp);
                        hasFinished.set(true);
                        //检测是否有奖励
                        List<Integer> awardList = taskCfg.getGetItem();
                        //没有奖励的任务 默认直接完成并且已经领取奖励
                        if (awardList.isEmpty() && taskCfg.getIntegralNum() <= TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD) {
                            taskDetail.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
                            taskDetail.setRewardTime(timestamp);
                            taskLogger.receiveTaskAward(playerId, taskDetail.getConfigId(), null, taskCfg.getIntegralNum());
                            log.info("玩家[{}]完成任务[{}]任务没有奖励直接修改为已领取状态!", playerId, taskDetail.getConfigId());
                        } else {
                            taskDetail.setStatus(TaskConstant.TaskStatus.STATUS_COMPLETED);
                            taskLogger.completeTask(playerId, taskDetail.getConfigId());
                            log.info("玩家[{}]完成任务[{}]", playerId, taskDetail.getConfigId());
                        }
                    }
                    updateTasks.add(Pair.newPair(taskData.getTaskDetail(taskCfg.getId()), taskCfg));
                }
            }
        } catch (Exception e) {
            log.error("玩家增加任务进度异常 playerId:{} param:{}", playerId, param, e);
        }
        if (isNotify && CollectionUtil.isNotEmpty(updateTasks)) {
            return hasFinished.get();
            //通知进度更新
//            noticeUpdate(playerId, updateTasks);
        }
        return false;
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
     * 添加新任务
     *
     * @param playerId    玩家ID
     * @param taskManager 任务管理器
     */
    private boolean addNewTasks(long playerId, TaskData tmpData, TaskManager taskManager) {
        // 获取当前可接取的任务配置列表
        List<TaskCfg> availableTaskConfigs = getAvailableTaskConfigs(taskManager.getTaskCfgMap());
        // 筛选出需要新增的任务
        Map<Integer, TaskDetail> newTasks = createNewTasksForPlayer(playerId, tmpData, availableTaskConfigs, taskManager);
        //有任务才更新
        if (!newTasks.isEmpty()) {
            //覆盖缓存
            newTasks.forEach((k, v) -> {
                //记录领取任务日志
                taskLogger.receiveTask(playerId, k);
//                log.info("玩家[{}]领取到[{}]任务", playerId, k);
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
    private List<TaskCfg> getAvailableTaskConfigs(Map<Integer, List<TaskCfg>> taskCfgMap) {
        List<TaskCfg> taskCfgList = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();

        //筛选出每个组中可以接取的任务
        taskCfgMap.forEach((groupId, list) -> {
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
     * @param availableTaskConfigs 可用任务配置列表
     * @param taskManager 任务管理器
     * @return 新创建的任务列表
     */
    private Map<Integer, TaskDetail> createNewTasksForPlayer(long playerId, TaskData taskData, List<TaskCfg> availableTaskConfigs, TaskManager taskManager) {
        if (availableTaskConfigs.isEmpty()) {
            return Collections.emptyMap();
        }

        if (taskData == null) {
            return Collections.emptyMap();
        }

        Map<Integer, TaskDetail> receiveMap = new HashMap<>();
        //检测是否有新任务
        availableTaskConfigs.forEach(taskCfg -> {
            List<Integer> taskCfgTaskConditionId = taskCfg.getTaskConditionId();
            if (taskCfgTaskConditionId != null && !taskCfgTaskConditionId.isEmpty()) {
                int conditionId = taskCfgTaskConditionId.getFirst();
                AbstractTaskCondition<DefaultTaskConditionParam> abstractTaskCondition = taskManager.getTaskCondition(conditionId);
                //有处理器才给玩家领取任务
                if (abstractTaskCondition != null) {
                    //不重复领取任务
                    if (!taskData.hasTask(taskCfg.getId())) {
                        TaskDetail taskDetail = createNewTaskDetail(playerId, taskCfg.getId());
                        taskData.addTaskDetail(taskDetail);
                        receiveMap.put(taskDetail.getConfigId(), taskDetail);
                    }
                } else {
                    log.warn("配置任务[{}],条件[{}]没有处理器!", taskCfg.getId(), conditionId);
                }
            }
        });

        return receiveMap;
    }

    /**
     * 创建新的任务数据
     *
     * @param playerId 玩家ID
     * @param taskId   任务配置ID
     * @return 新的任务数据
     */
    private TaskDetail createNewTaskDetail(long playerId, int taskId) {
        TaskDetail taskDetail = new TaskDetail();
        taskDetail.setConfigId(taskId);
        taskDetail.setStatus(TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        taskDetail.setCreateTime(System.currentTimeMillis());
        taskDetail.setPlayerId(playerId);
        return taskDetail;
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


    public void moveToMongo(long playerId) {
        RMap<Long, TaskData> playerTasks = getPlayerTaskMap();
        redisLock.lockAndRun(playerTaskMapLockKey(playerId), LOCK_TIME, () -> {
            TaskData taskData = playerTasks.get(playerId);
            if (taskData == null) {
                return;
            }
            taskDataDao.save(taskData);
            playerTasks.remove(playerId);
        });
    }

}