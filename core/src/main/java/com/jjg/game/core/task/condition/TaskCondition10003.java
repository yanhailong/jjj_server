package com.jjg.game.core.task.condition;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.db.TaskData;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.core.task.param.TaskConditionParam10003;
import com.jjg.game.sampledata.bean.TaskCfg;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 游戏实际赢钱
 */
@Component
public class TaskCondition10003 extends AbstractTaskCondition<TaskConditionParam10003> {
    /**
     * 获取任务的条件ID。
     */
    @Override
    protected int getConditionId() {
        return TaskConstant.ConditionType.PLAY_GAME_WIN_MONEY;
    }

    @Override
    protected boolean checkAddProgress(TaskCfg taskCfg, TaskDetail taskDetail, TaskConditionParam10003 param) {
        List<Long> conditionId = taskCfg.getTaskConditionId();
        long gameId = conditionId.get(1);
        long checkValue = conditionId.get(2);
        long coinId = conditionId.get(4);
        //货币类型不匹配
        if (coinId != param.getCoinId()) {
            return false;
        }
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
        return taskCfg.getTaskConditionId().get(3);
    }
}
