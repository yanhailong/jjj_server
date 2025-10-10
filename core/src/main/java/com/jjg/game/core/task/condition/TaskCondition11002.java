package com.jjg.game.core.task.condition;

import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

/**
 * 累计充值条件
 */
@Component
public class TaskCondition11002 extends AbstractTaskCondition<DefaultTaskConditionParam> {

    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return 11002;
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg 任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    @Override
    protected Long getCompareValue(TaskCfg taskCfg) {
        return Long.valueOf(taskCfg.getTaskConditionId().get(1));
    }

}
