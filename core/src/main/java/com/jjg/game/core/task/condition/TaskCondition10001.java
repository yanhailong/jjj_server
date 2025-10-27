package com.jjg.game.core.task.condition;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.param.TaskConditionParam10001;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 投注次数
 */
@Component
public class TaskCondition10001 extends AbstractTaskCondition<TaskConditionParam10001> {
    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return TaskConstant.ConditionType.BET_COUNT;
    }

    @Override
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskData taskData, TaskConditionParam10001 param) {
        List<Integer> conditionId = taskCfg.getTaskConditionId();
        int gameId = conditionId.get(1);
        int checkValue = conditionId.get(2);
        if (gameId > 0) {
            if (param.getGameId() != gameId) {
                return false;
            }
        }
        return checkValue <= param.getAddValue();
    }

    /**
     * 获取任务条件的比较值，用于判断任务完成条件是否达成。
     *
     * @param taskCfg 任务配置信息，包含任务必要的元数据。
     * @return 返回用于比较的具体值，例如任务完成所需的目标数量或指标。
     */
    @Override
    protected Long getCompareValue(TaskCfg taskCfg) {
        return Long.valueOf(taskCfg.getTaskConditionId().get(3));
    }

    /**
     * 增加任务进度
     */
    @Override
    protected void addProgress(TaskCfg taskCfg, TaskData taskData, TaskConditionParam10001 param) {
        Map<Integer, Long> taskDataProgress = taskData.getProgress();
        //下注次数 每次只+1
        taskDataProgress.merge(taskCfg.getTaskConditionId().getFirst(), 1L, Long::sum);
    }
}
