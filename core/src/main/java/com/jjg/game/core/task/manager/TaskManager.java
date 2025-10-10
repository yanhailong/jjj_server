package com.jjg.game.core.task.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.task.condition.AbstractTaskCondition;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.core.task.service.TaskService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 任务管理器
 * 负责任务的触发、进度更新、完成检查和奖励发放
 */
@Component
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private final TaskService taskService;

    /**
     * 保存任务条件映射关系的变量。
     */
    private final Map<Integer, AbstractTaskCondition<DefaultTaskConditionParam>> taskConditionMap = new HashMap<>();

    /**
     * 根据条件id分组 k=条件id
     */
    private final Map<Integer, List<TaskCfg>> taskCfgMap = new HashMap<>();

    public TaskManager(TaskService taskService) {
        this.taskService = taskService;
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
            taskService.init(this);
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
                taskCfgMap.clear();
                configList.forEach(taskCfg -> taskCfgMap.computeIfAbsent(taskCfg.getTaskConditionId().getFirst(), k -> new ArrayList<>())
                        .add(taskCfg));
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

    /**
     * 触发任务条件
     *
     * @param playerId    玩家id
     * @param conditionId 条件id
     * @param param       条件参数
     */
    public void trigger(long playerId, int conditionId, Supplier<DefaultTaskConditionParam> param) {
        if (param == null) {
            return;
        }
        DefaultTaskConditionParam conditionParam = param.get();
        if (conditionParam == null) {
            return;
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
        try {
            taskService.trigger(playerId, taskConfigs, taskCondition, conditionParam);
        } catch (Exception e) {
            log.error("玩家[{}]任务条件[{}]触发失败!", playerId, conditionId, e);
        }
    }

}