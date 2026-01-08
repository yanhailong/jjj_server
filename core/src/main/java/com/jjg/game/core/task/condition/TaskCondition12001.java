package com.jjg.game.core.task.condition;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.TaskConditionParam12001;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 有效下注
 */
@Component
public class TaskCondition12001 extends AbstractTaskCondition<TaskConditionParam12001> {
    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return TaskConstant.ConditionType.PLAYER_BET_ALL;
    }

    @Override
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskDetail taskDetail, TaskConditionParam12001 param) {
        List<Long> conditionId = taskCfg.getTaskConditionId();
        long gameId = conditionId.get(1);
        if (gameId > 0) {
            return param.getGameId() == gameId;
        }
        return true;
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg  任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    @Override
    protected Long getCompareValue(TaskCfg taskCfg) {
        return taskCfg.getTaskConditionId().get(2);
    }

}
