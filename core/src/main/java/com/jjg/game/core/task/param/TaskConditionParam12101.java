package com.jjg.game.core.task.param;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;

/**
 * 使用道具任务参数
 */
public class TaskConditionParam12101 extends DefaultTaskConditionParam {

    /**
     * 道具id
     */
    private int itemId;
    private ActionConditionEvent conditionEvent;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
        conditionEvent = null;
    }

    @Override
    public void setAddValue(long addValue) {
        super.setAddValue(addValue);
        conditionEvent = null;
    }

    /** 每次业务触发只构造一个不可变事实事件，供同条件下的多个任务配置复用。 */
    public ActionConditionEvent conditionEvent() {
        if (conditionEvent == null) {
            conditionEvent = new ActionConditionEvent(
                    ActionConditionEvent.Type.ITEM_USE, itemId, 0, addValue, addValue, 0, false);
        }
        return conditionEvent;
    }
}
