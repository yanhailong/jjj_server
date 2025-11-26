package com.jjg.game.core.task.condition;

import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.core.task.pb.TaskCondition;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * 任务条件父类
 */
public abstract class AbstractTaskCondition<T extends DefaultTaskConditionParam> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected AbstractTaskCondition() {
        init();
    }

    /**
     * 初始化任务进度 有需要才重写
     */
    protected void init() {
    }

    /**
     * 获取任务条件的唯一ID。
     *
     * @return 返回当前任务条件的ID。
     */
    public int getId() {
        return getConditionId();
    }

    /**
     * 获取任务的条件ID。
     */
    protected abstract int getConditionId();

    /**
     * 触发任务条件
     *
     * @param param 条件参数
     * @return true 进度有变化
     */
    public boolean trigger(long playerId, TaskCfg taskCfg, TaskDetail taskDetail, T param) {
        boolean trigger = false;
        Set<Integer> finishConditions = taskDetail.getFinishConditionIds();
        //已完成的条件不增加进度了
        if (finishConditions != null && finishConditions.contains(taskCfg.getTaskConditionId().getFirst())) {
            return trigger;
        }
        if (checkAddProgress(taskCfg, taskDetail, param)) {
            addProgress(playerId, taskCfg, taskDetail, param);
            trigger = true;
        }
        boolean finish = checkFinish(taskCfg, taskDetail, param);
        if (finish) {
            taskDetail.getFinishConditionIds().add(taskCfg.getTaskConditionId().getFirst());
        }
        return trigger;
    }

    /**
     * 检查是否满足增加任务进度的条件。(默认true)
     *
     * @param param 条件参数，包含触发条件的相关数据。
     * @return 如果满足增加任务进度的条件，返回true；否则返回false。
     */
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskDetail taskDetail, T param) {
        return true;
    }

    /**
     * 增加任务进度
     */
    protected void addProgress(long playerId, TaskCfg taskCfg, TaskDetail taskDetail, T param) {
        Map<Integer, Long> taskDataProgress = taskDetail.getProgress();
        taskDataProgress.merge(taskCfg.getTaskConditionId().getFirst(), param.getAddValue(), Long::sum);
        log.debug("玩家[{}]任务[{}]条件[{}]增加进度[{}]总进度[{}]", playerId, taskCfg.getId(), getConditionId(), param.getAddValue(), taskDataProgress.get(taskCfg.getTaskConditionId().getFirst()));
    }

    /**
     * 获取进度
     *
     * @param taskCfg    任务配置
     * @param taskDetail 任务数据
     * @param param      参数
     * @return
     */
    protected Long getProgress(TaskCfg taskCfg, TaskDetail taskDetail, T param) {
        Map<Integer, Long> taskDataProgress = taskDetail.getProgress();
        return taskDataProgress.getOrDefault(taskCfg.getTaskConditionId().getFirst(), 0L);
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg 任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    protected abstract Long getCompareValue(TaskCfg taskCfg);

    /**
     * 检测任务进度是否完成
     *
     * @return
     */
    public boolean checkFinish(TaskCfg taskCfg, TaskDetail taskDetail, T param) {
        long progress = getProgress(taskCfg, taskDetail, param);
        long compareValue = getCompareValue(taskCfg);
        return progress >= compareValue;
    }

    /**
     * 组装条件消息体
     *
     * @param taskDetail 任务数据
     * @param taskCfg    任务配置
     * @return 条件协议体
     */
    public TaskCondition assembleTaskCondition(TaskDetail taskDetail, TaskCfg taskCfg) {
        Set<Integer> finishConditionIds = taskDetail.getFinishConditionIds();
        boolean finish = finishConditionIds != null && finishConditionIds.contains(getConditionId());
        Map<Integer, Long> taskDataProgress = taskDetail.getProgress();
        TaskCondition taskCondition = new TaskCondition();
        taskCondition.setProgress(taskDataProgress.getOrDefault(getConditionId(), 0L));
        taskCondition.setConfigParam(getCompareValue(taskCfg));
        taskCondition.setFinish(finish);
        taskCondition.setConfigId(getConditionId());
        return taskCondition;
    }

}