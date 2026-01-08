package com.jjg.game.core.task.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.listener.OnSwitchNode;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.logger.TaskLogger;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.task.condition.AbstractTaskCondition;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.core.task.pb.res.NotifyUpdateTask;
import com.jjg.game.core.task.service.TaskService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 任务管理器
 * 全部存放在内存中
 * 负责任务的触发、进度更新、完成检查和奖励发放
 */
@Component
public class TaskManager implements ConfigExcelChangeListener, IRedDotService, OnSwitchNode {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private final TaskService taskService;
    private final PlayerPackService playerPackService;
    private final TaskLogger taskLogger;
    private final RedDotManager redDotManager;
    private final ClusterSystem clusterSystem;

    /**
     * 保存任务条件映射关系的变量。
     */
    private final Map<Integer, AbstractTaskCondition<DefaultTaskConditionParam>> taskConditionMap = new HashMap<>();
    /**
     * 玩家任务数据
     */
    private final Map<Long, TaskData> playerTaskMap = new ConcurrentHashMap<>();

    /**
     * 根据条件id分组 k=条件id
     */
    private Map<Integer, List<TaskCfg>> taskCfgMap = new HashMap<>();

    /**
     * 根据条件id分组 k=条件id
     */
    private Map<Integer, List<TaskCfg>> taskGroupMap = new HashMap<>();

    public TaskManager(TaskService taskService, PlayerPackService playerPackService, TaskLogger taskLogger, RedDotManager redDotManager, ClusterSystem clusterSystem) {
        this.taskService = taskService;
        this.playerPackService = playerPackService;
        this.taskLogger = taskLogger;
        this.redDotManager = redDotManager;
        this.clusterSystem = clusterSystem;
    }

    public Map<Integer, List<TaskCfg>> getTaskCfgMap() {
        return taskCfgMap;
    }

    public Map<Integer, List<TaskCfg>> getTaskGroupMap() {
        return taskGroupMap;
    }

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::initTaskConfig);
    }

    /**
     * 初始化任务管理器
     */
    public void init() {
        try {
            // 初始化任务数据
            initTaskConfig();
            //初始化任务条件处理器
            initCondition();
            //初始化任务服务
            log.info("任务管理器初始化成功");
        } catch (Exception e) {
            log.error("任务管理器初始化失败", e);
        }
    }

    /**
     * 初始化任务数据
     */
    private void initTaskConfig() {
        try {
            // 确保所有任务配置都已加载
            List<TaskCfg> configList = GameDataManager.getTaskCfgList();
            if (configList != null && !configList.isEmpty()) {
                Map<Integer, List<TaskCfg>> tempMap = new HashMap<>();
                Map<Integer, List<TaskCfg>> tempGroupMap = new HashMap<>();
                configList.forEach(taskCfg -> {
                    tempMap.computeIfAbsent(taskCfg.getTaskConditionId().getFirst().intValue(), k -> new ArrayList<>())
                            .add(taskCfg);
                    tempGroupMap.computeIfAbsent(taskCfg.getGroup(), k -> new ArrayList<>())
                            .add(taskCfg);
                });
                taskCfgMap = tempMap;
                taskGroupMap = tempGroupMap;
                log.info("成功加载了[{}]个任务配置", configList.size());
            } else {
                log.warn("没有加载到任何任务配置");
            }
        } catch (Exception e) {
            log.error("初始化任务数据失败", e);
        }
    }

    /**
     * 初始化任务条件解析器
     */
    @SuppressWarnings("unchecked")
    public void initCondition() {
        CommonUtil.getContext().getBeansOfType(AbstractTaskCondition.class).forEach((k, v) -> {
            taskConditionMap.put(v.getId(), v);
        });
    }

    /**
     * 获取指定条件ID对应的任务条件处理器。
     *
     * @param conditionId 条件ID，用于标识任务条件。
     * @return 返回对应的任务条件处理器，如果条件ID不存在，返回null。
     */
    public AbstractTaskCondition<DefaultTaskConditionParam> getTaskCondition(int conditionId) {
        return taskConditionMap.get(conditionId);
    }

    public void trigger(long playerId, int conditionId, Supplier<DefaultTaskConditionParam> param) {
        trigger(playerId, conditionId, param, true);
    }

    /**
     * 触发任务条件
     *
     * @param playerId    玩家id
     * @param conditionId 条件id
     * @param param       条件参数
     */
    public void trigger(long playerId, int conditionId, Supplier<DefaultTaskConditionParam> param, boolean isNotify) {
        if (param == null) {
            return;
        }
        try {

            DefaultTaskConditionParam conditionParam = param.get();
            if (conditionParam == null) {
                return;
            }
            TaskData taskData = playerTaskMap.get(playerId);
            if (taskData == null) {
                log.error("玩家更新任务进度时 任务数据为null playerId:{} conditionId:{} param:{}", playerId, conditionId, param.get());
                return;
            }
            //检查是否需要重置所有任务
            if (!TimeHelper.inSameDay(taskData.getLastCheckTime(), System.currentTimeMillis())) {
                taskService.checkTask(playerId, taskData, this);
            }
            //根据条件id计算出受影响的任务
            List<TaskCfg> taskConfigs = taskCfgMap.get(conditionId);
            //无事发生
            if (taskConfigs == null || taskConfigs.isEmpty()) {
                return;
            }
            AbstractTaskCondition<DefaultTaskConditionParam> taskCondition = getTaskCondition(conditionId);
            //无事发生
            if (taskCondition == null) {
                log.info("触发[{}]条件,但是没有对应的条件处理器!", conditionId);
                return;
            }
            boolean needRed = taskService.trigger(playerId, taskData, taskConfigs, taskCondition, conditionParam, isNotify);
            if (needRed) {
                updateRedDot(playerId);
            }
        } catch (Exception e) {
            log.error("玩家[{}]任务条件[{}]触发失败!", playerId, conditionId, e);
        }
    }

    public void updateRedDot(long playerId) {
        redDotManager.updateRedDotByInitialize(getModule(), getSubmodule(), playerId);
    }

    /**
     * 通知玩家任务更新
     */
    public void noticeUpdate(long playerId, TaskDetail taskDetail, TaskCfg config) {
        Task task = assembleTask(playerId, taskDetail, config);
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
    public void noticeUpdate(long playerId, List<Pair<TaskDetail, TaskCfg>> updateTasks) {
        List<Task> list = new ArrayList<>(updateTasks.size());
        for (Pair<TaskDetail, TaskCfg> updateTask : updateTasks) {
            Task task = assembleTask(playerId, updateTask.getFirst(), updateTask.getSecond());
            list.add(task);
        }
        NotifyUpdateTask updateTask = new NotifyUpdateTask();
        updateTask.setTaskList(list);
        PFSession pfSession = clusterSystem.getSession(playerId);
        if (pfSession != null) {
            pfSession.send(updateTask);
        }
    }

    /**
     * 组装任务消息
     *
     * @param taskDetail 任务数据
     * @param config     配置
     * @return 任务消息体
     */
    public Task assembleTask(long playerId, TaskDetail taskDetail, TaskCfg config) {
        Task task = new Task();
        task.setConfigId(config.getId());
        //任务状态
        task.setStatus(taskDetail.getStatus());
        //任务条件
        task.setConditions(assembleTaskConditions(playerId, taskDetail, config));
        return task;
    }

    /**
     * 通知玩家任务更新
     */
    public void noticeUpdateAll(long playerId) {
        NotifyUpdateTask updateTask = new NotifyUpdateTask();
        TaskData taskData = playerTaskMap.get(playerId);
        if (taskData == null || taskData.getTaskDetails() == null || taskData.getTaskDetails().isEmpty()) {
            return;
        }
        //已领取任务不显示
        List<Task> tasks = taskData.getTaskDetails().values().stream().map(detail -> {
            if (detail.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED) {
                return null;
            }
            TaskCfg taskCfg = GameDataManager.getTaskCfg(detail.getConfigId());
            if (taskCfg == null) {
                return null;
            }
            return assembleTask(playerId, detail, taskCfg);
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

    /**
     * 获取玩家任务列表
     *
     * @param playerId 任务列表
     * @return 任务列表
     */
    public List<Task> getPlayerTaskList(long playerId, int type) {
        List<Task> taskList = new ArrayList<>();
        TaskData taskData = playerTaskMap.get(playerId);
        if (taskData == null) {
            return taskList;
        }
        //检查是否需要重置所有任务
        if (!TimeHelper.inSameDay(taskData.getLastCheckTime(), System.currentTimeMillis())) {
            taskService.checkTask(playerId, taskData, this);
        }
        if (taskData.getTaskDetails() == null || taskData.getTaskDetails().isEmpty()) {
            return taskList;
        }
        Map<Integer, TaskManager.TaskSortParam> map = new HashMap<>(taskData.getTaskDetails().size());
        //生成数据
        for (TaskDetail taskDetail : taskData.getTaskDetails().values()) {
            //已领取任务不显示
            if (taskDetail.getStatus() == TaskConstant.TaskStatus.STATUS_REWARDED) {
                continue;
            }
            TaskCfg taskCfg = GameDataManager.getTaskCfg(taskDetail.getConfigId());
            if (taskCfg == null) {
                continue;
            }
            if (taskCfg.getTaskType() != type) {
                continue;
            }
            Task task = assembleTask(playerId, taskDetail, taskCfg);
            long minProgress = 100;
            if (taskDetail.getStatus() != TaskConstant.TaskStatus.STATUS_COMPLETED) {
                for (TaskCondition condition : task.getConditions()) {
                    long progress = condition.getProgress() * 100 / condition.getConfigParam();
                    minProgress = Math.min(minProgress, progress);
                }
            }
            map.put(taskDetail.getConfigId(), new TaskManager.TaskSortParam(task, taskDetail, minProgress));
        }
        if (map.isEmpty()) {
            return taskList;
        }
        //排序
        return map.values().stream().sorted((o1, o2) -> {
            TaskDetail taskDetail1 = o1.taskDetail;
            TaskDetail taskDetail2 = o2.taskDetail;
            int o1Status = (taskDetail1.getStatus() + 1) % 3;
            int o2Status = (taskDetail2.getStatus() + 1) % 3;
            int compare = Integer.compare(o1Status, o2Status);
            if (compare == 0) {
                switch (taskDetail1.getStatus()) {
                    case TaskConstant.TaskStatus.STATUS_COMPLETED -> {
                        return -Long.compare(taskDetail1.getCompleteTime(), taskDetail2.getCompleteTime());
                    }
                    case TaskConstant.TaskStatus.STATUS_IN_PROGRESS -> {
                        int compared = Long.compare(o1.progress, o2.progress);
                        if (compared == 0) {
                            return -Integer.compare(taskDetail1.getConfigId(), taskDetail2.getConfigId());
                        } else {
                            return -compared;
                        }
                    }
                    case TaskConstant.TaskStatus.STATUS_REWARDED -> {
                        return 1;
                    }
                }
            }
            return -compare;
        }).map(TaskManager.TaskSortParam::task).toList();
    }

    @Override
    public void onSwitchNodeAction(PFSession pfSession) {
        if (pfSession == null || pfSession.getPlayerId() <= 0) {
            return;
        }
        onExit(pfSession.playerId);
    }

    /**
     * 关服执行
     */
    public void shutdown() {
        for (Map.Entry<Long, TaskData> dataEntry : playerTaskMap.entrySet()) {
            taskService.saveTask(dataEntry.getKey(), dataEntry.getValue());
            log.info("关服玩家回存任务信息成功 playerId:{}", dataEntry.getKey());
        }
    }

    record TaskSortParam(Task task, TaskDetail taskDetail, long progress) {
    }


    /**
     * 组装任务条件消息体
     *
     * @param taskDetail 任务数据
     * @param taskCfg    任务配置
     * @return 任务条件消息体
     */
    public List<TaskCondition> assembleTaskConditions(long playerId, TaskDetail taskDetail, TaskCfg taskCfg) {
        List<TaskCondition> conditions = new ArrayList<>();
        Map<Integer, Long> taskDataProgress = taskDetail.getProgress();
        // 判断进度是否为空，统一处理
        if (taskDataProgress == null || taskDataProgress.isEmpty()) {
            addTaskConditionToList(playerId, taskCfg.getTaskConditionId().getFirst().intValue(), taskDetail, taskCfg, conditions);
        } else {
            taskDataProgress.forEach((taskId, progress) -> addTaskConditionToList(playerId, taskId, taskDetail, taskCfg, conditions));
        }
        return conditions;
    }

    private void addTaskConditionToList(long playerId, int conditionId, TaskDetail taskDetail, TaskCfg taskCfg, List<TaskCondition> conditions) {
        try {
            AbstractTaskCondition<DefaultTaskConditionParam> taskCondition = taskConditionMap.get(conditionId);
            if (taskCondition != null) {
                TaskCondition condition = taskCondition.assembleTaskCondition(taskDetail, taskCfg);
                conditions.add(condition);
            } else {
                log.warn("任务[{}]条件[{}]找不到条件处理器! playerId={}",
                        taskDetail.getConfigId(), conditionId, playerId);
            }
        } catch (Exception e) {
            log.error("组装任务条件失败 taskId={}, conditionId={}, playerId={}, error={}",
                    taskDetail.getConfigId(), conditionId, playerId, e.getMessage(), e);
        }
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
        TaskData taskData = playerTaskMap.get(playerId);
        if (taskData == null || taskData.getTaskDetails() == null || taskData.getTaskDetails().isEmpty()) {
            log.error("玩家[{}]任务数据taskData为空", playerId);
            return false;
        }
        try {
            TaskDetail taskDetail = taskData.getTaskDetail(taskId);
            //状态不对
            if (taskDetail == null || taskDetail.getStatus() != TaskConstant.TaskStatus.STATUS_COMPLETED) {
                log.warn("玩家[{}]领取[{}]任务奖励失败!任务状态[{}]", playerId, taskId, taskDetail == null ? "null" : taskDetail.getStatus());
                return false;
            }
            //修改任务状态
            taskDetail.setRewardTime(System.currentTimeMillis());
            taskDetail.setStatus(TaskConstant.TaskStatus.STATUS_REWARDED);
            List<Integer> getItem = taskCfg.getGetItem();
            int integralNum = taskCfg.getIntegralNum();
            //如果有积分奖励通知大厅
            if (integralNum > TaskConstant.TimeConstants.MIN_INTEGRAL_REWARD) {
                taskService.addPlayerPoints(playerId, integralNum, true);
            }
            List<Item> itemList = new ArrayList<>();
            if (!getItem.isEmpty()) {
                itemList = ItemUtils.buildItems(getItem);
                playerPackService.addItems(playerId, itemList, AddType.TASKAWARD);
            }
            //记录日志
            taskLogger.receiveTaskAward(playerId, taskId, itemList, integralNum, TaskConstant.TaskStatus.STATUS_REWARDED);
            log.info("玩家[{}]成功领取任务[{}]奖励", playerId, taskId);
            updateRedDot(playerId);
            return true;
        } catch (Exception e) {
            log.error("领取任务奖励失败 playerId={}, taskId={}, error={}", playerId, taskId, e.getMessage(), e);
            return false;
        }
    }

    public void onExit(long playerId) {
        TaskData taskData = playerTaskMap.remove(playerId);
        if (taskData != null) {
            //回存到redis
            taskService.saveTask(playerId, taskData);
            log.info("玩家回存任务信息成功 playerId:{}", playerId);
        } else {
            log.info("sessionClose 时保存任务数据错误 playerId={}", playerId);
        }
    }

    public void saveTask(long playerId) {
        TaskData taskData = playerTaskMap.get(playerId);
        if (taskData != null) {
            //回存到redis
            taskService.saveTask(playerId, taskData);
            log.info("主动回存玩家任务信息成功 playerId:{}", playerId);
        } else {
            log.info("主动回存玩家 时保存任务数据错误 playerId={}", playerId);
        }
    }


    public void loadTaskData(long playerId) {
        playerTaskMap.computeIfAbsent(playerId, k -> {
            log.info("玩家从redis加载任务信息成功 playerId:{}", playerId);
            return taskService.getPlayerTask(playerId);
        });
        log.info("玩家加载任务信息成功 playerId:{}", playerId);
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
        TaskData taskData = playerTaskMap.get(playerId);
        if (taskData == null || taskData.getTaskDetails() == null || taskData.getTaskDetails().isEmpty()) {
            return Collections.emptyList();
        }
        List<RedDotDetails> redDotList = new ArrayList<>();
        RedDotDetails redDotDetails = new RedDotDetails();
        redDotDetails.setRedDotType(RedDotDetails.RedDotType.COUNT);
        redDotDetails.setRedDotModule(RedDotDetails.RedDotModule.TASK);
        redDotList.add(redDotDetails);
        //红点子模块
        redDotDetails.setRedDotSubmodule(submodule);
        List<TaskDetail> tasks = taskData.getTaskDetails().values().stream().filter(taskDetail -> taskDetail.getStatus() == TaskConstant.TaskStatus.STATUS_COMPLETED).toList();
        if (!tasks.isEmpty()) {
            List<TaskDetail> pointAwardTasks = tasks;
            if (submodule != 0) {
                //筛选任务
                pointAwardTasks = tasks.stream()
                        .filter(detail -> {
                            TaskCfg taskCfg = GameDataManager.getTaskCfg(detail.getConfigId());
                            return taskCfg.getTaskType() == submodule;
                        })
                        .toList();
            }
            if (!pointAwardTasks.isEmpty()) {
                List<Integer> taskIds = tasks.stream().map(TaskDetail::getConfigId).toList();
                redDotDetails.setCount(taskIds.size());
                JSONObject extra = new JSONObject();
                extra.put("taskIds", taskIds);
                redDotDetails.setExtra(extra.toJSONString());
            }
        }
        return redDotList;
    }
}