package com.jjg.game.core.task.condition;

import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.param.TaskConditionParam12101;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

/**
 * 使用道具
 */
@Component
public class TaskCondition12101 extends AbstractTaskCondition<TaskConditionParam12101> {

    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return 12101;
    }

    /**
     * 检查是否满足增加任务进度的条件。(默认true)
     *
     * @param param 条件参数，包含触发条件的相关数据。
     * @return 如果满足增加任务进度的条件，返回true；否则返回false。
     */
    @Override
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskData taskData, TaskConditionParam12101 param) {
        int itemId = taskCfg.getTaskConditionId().get(1);
        return param.getItemId() == itemId;
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg 任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    @Override
    protected Long getCompareValue(TaskCfg taskCfg) {
        return Long.valueOf(taskCfg.getTaskConditionId().get(2));
    }

}
