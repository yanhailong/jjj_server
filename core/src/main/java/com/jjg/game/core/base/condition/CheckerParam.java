package com.jjg.game.core.base.condition;

/**
 * 检查参数
 *
 * @author 2CL
 */
public class CheckerParam {
    /**
     * 检查名
     */
    private final String checkName;

    /**
     * 目标参数
     */
    private final Object targetParam;

    /**
     * 条件比较器
     */
    private EConditionComparator comparator = EConditionComparator.EQ;

    public CheckerParam(String checkName, Object targetParam) {
        this.checkName = checkName;
        this.targetParam = targetParam;
    }

    public String getCheckName() {
        return checkName;
    }

    public Object getTargetParam() {
        return targetParam;
    }

    public EConditionComparator getComparator() {
        return comparator;
    }

    public void setComparator(EConditionComparator comparator) {
        this.comparator = comparator;
    }
}
