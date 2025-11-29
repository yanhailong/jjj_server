package com.jjg.game.core.task.param;

/**
 * 默认任务条件参数
 */
public class DefaultTaskConditionParam {

    /**
     * 增加值
     */
    protected long addValue;

    /**
     * 结果值
     */
    protected long resultValue;

    public long getAddValue() {
        return addValue;
    }

    public void setAddValue(long addValue) {
        this.addValue = addValue;
    }

    public long getResultValue() {
        return resultValue;
    }

    public void setResultValue(long resultValue) {
        this.resultValue = resultValue;
    }

    @Override
    public String toString() {
        return "DefaultTaskConditionParam{" +
                "addValue=" + addValue +
                ", resultValue=" + resultValue +
                '}';
    }
}
