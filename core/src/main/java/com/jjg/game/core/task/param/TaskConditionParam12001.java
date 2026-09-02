package com.jjg.game.core.task.param;

import com.jjg.game.core.base.condition.numeric.GameConditionEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 有效下注参数
 */
public class TaskConditionParam12001 extends DefaultTaskConditionParam {

    /**
     * 游戏id
     */
    private int gameId;
    private GameConditionEvent conditionEvent;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
        conditionEvent = null;
    }

    @Override
    public void setAddValue(long addValue) {
        super.setAddValue(addValue);
        conditionEvent = null;
    }

    /** 每次业务触发只构造一个不可变事实事件，供同条件下的多个任务配置复用。 */
    public GameConditionEvent conditionEvent() {
        if (conditionEvent == null) {
            conditionEvent = new GameConditionEvent(
                    gameId, gameId, 0, 0, 0, addValue, 0, 0,
                    addValue > 0, true, 0, Map.of(), 0, Set.of(), List.of(), Map.of());
        }
        return conditionEvent;
    }
}
