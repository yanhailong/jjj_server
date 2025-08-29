package com.jjg.game.core.base.condition;

import com.jjg.game.core.base.gameevent.EGameEventType;

import java.util.List;

/**
 * 条件
 *
 * @author 2CL
 */
public interface IConditionChecker {

    /**
     * 绑定的条件检查类型
     */
    String bindConditionCheckType();


    /**
     * 绑定的条件检查参数，Condition表中的ConditionType字段
     */
    String bindConditionCheckParam();

    /**
     * 根据绑定的条件检查参数，获取对应的目标检查param
     */
    default CheckerParam filterBindParamCheckParam(List<CheckerParam> checkerParams) {
        String bindConditionCheckParam = bindConditionCheckParam();
        for (CheckerParam checkerParam : checkerParams) {
            if (bindConditionCheckParam.equalsIgnoreCase(checkerParam.getCheckName())) {
                return checkerParam;
            }
        }
        return null;
    }
}
