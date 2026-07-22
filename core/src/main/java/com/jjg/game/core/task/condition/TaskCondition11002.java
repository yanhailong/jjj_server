package com.jjg.game.core.task.condition;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.TaskConditionParamRecharge;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

/**
 * 累计充值条件
 */
@Component
public class TaskCondition11002 extends AbstractTaskCondition<TaskConditionParamRecharge> {
    private final TaskConditionCache conditions;

    public TaskCondition11002(ConditionRuleRegistry conditionRules) {
        this.conditions = new TaskConditionCache(conditionRules);
    }

    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return TaskConstant.ConditionType.PLAYER_SUM_PAY;
    }

    @Override
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskDetail taskDetail, TaskConditionParamRecharge param) {
        ConditionUpdate update = condition(taskCfg).evaluate(param.conditionEvent());
        return update.matched() && update.value() > 0;
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg 任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    @Override
    protected Long getCompareValue(TaskCfg taskCfg) {
        return condition(taskCfg).target();
    }

    private PreparedCondition condition(TaskCfg taskCfg) {
        return conditions.get(taskCfg);
    }
}
