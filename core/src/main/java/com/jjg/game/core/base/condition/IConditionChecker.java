package com.jjg.game.core.base.condition;

import java.util.HashSet;
import java.util.List;

/**
 * 条件
 *
 * @author 2CL
 */
public interface IConditionChecker {

    /**
     * 绑定的条件检查类型，一般用 Condition表中的TriggerEventType字段 进行表示，也可自定义类型，只要在check方法中用到就行
     * !!! 不要使用 EGameEventType 枚举替代 ！！！
     */
    String bindConditionCheckType();


    /**
     * 绑定的条件检查参数，一般用 Condition表中的ConditionType字段 进行表示，也可自定义类型，需要在触发调用时，写入此返回值
     * !!! 不要使用 EGameEventType 枚举替代 ！！！
     */
    List<String> bindConditionCheckParam();

    /**
     * 默认的条件比较器
     */
    EConditionComparator defaultConditionComparator();

    /**
     * 条件比较器
     */
    default EConditionComparator getConditionComparator(CheckerParam checkerParam) {
        return checkerParam.getComparator() != null
            ? checkerParam.getComparator()
            : defaultConditionComparator();
    }

    /**
     * 根据绑定的条件检查参数，获取对应的目标检查param
     */
    default CheckerParam filterBindParamCheckParam(List<CheckerParam> checkerParams) {
        List<String> bindConditionCheckParam = bindConditionCheckParam();
        for (CheckerParam checkerParam : checkerParams) {
            if (checkerParam.getCheckName().containsAll(bindConditionCheckParam)) {
                return checkerParam;
            }
        }
        return null;
    }
}
