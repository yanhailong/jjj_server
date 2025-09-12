package com.jjg.game.core.base.condition;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.String;

/**
 * 条件检查服务
 *
 * @author 2CL
 */
@Service
public class ConditionCheckService {

    private static final Logger log = LoggerFactory.getLogger(ConditionCheckService.class);
    /**
     * 条件检查map
     */
    private final Map<String, IConditionChecker> conditionCheckerMap = new HashMap<>();

    /**
     * 初始化条件检查
     */
    public void initConditionChecker() {
        Map<String, IGameConditionChecker> gameConditionCheckerMap =
                CommonUtil.getContext().getBeansOfType(IGameConditionChecker.class);
        for (Map.Entry<String, IGameConditionChecker> entry : gameConditionCheckerMap.entrySet()) {
            String bindConditionCheckType = entry.getValue().bindConditionCheckType();
            if (conditionCheckerMap.containsKey(bindConditionCheckType)) {
                log.error("重复添加类型：{} 的系统条件检查，{} =》{}",
                        bindConditionCheckType,
                        entry.getValue().getClass().getName(),
                        conditionCheckerMap.get(bindConditionCheckType).getClass().getName());
            }
            conditionCheckerMap.put(bindConditionCheckType, entry.getValue());
        }
        Map<String, IPlayerConditionChecker> playerConditionCheckerMap =
                CommonUtil.getContext().getBeansOfType(IPlayerConditionChecker.class);
        for (Map.Entry<String, IPlayerConditionChecker> entry : playerConditionCheckerMap.entrySet()) {
            String bindConditionCheckType = entry.getValue().bindConditionCheckType();
            if (conditionCheckerMap.containsKey(bindConditionCheckType)) {
                log.error("重复添加类型：{} 的玩家条件检查，{} =》{}",
                        bindConditionCheckType,
                        entry.getValue().getClass().getName(),
                        conditionCheckerMap.get(bindConditionCheckType).getClass().getName());
            }
            conditionCheckerMap.put(bindConditionCheckType, entry.getValue());
        }
    }

    /**
     * 是否触发成功
     *
     * @param checkParams 检查参数
     */
    public boolean isTriggerComplete(Object sourceTarget, Map<Integer, Integer> checkParams) {
        for (Map.Entry<Integer, Integer> entry : checkParams.entrySet()) {
            ConditionCfg cfg = GameDataManager.getConditionCfg(entry.getKey());
            if (cfg == null) {
                log.error("条件检查配置为空 id:{}", entry.getKey());
                return false;
            }
            List<CheckerParam> checkerParams =
                    Collections.singletonList(new CheckerParam(cfg.getConditionType(), entry.getValue()));
            if (!isTriggerComplete(sourceTarget, cfg, checkerParams)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 是否触发成功
     *
     * @param conditionCfg 触发条件配置
     * @param checkParams  检查参数
     */
    public boolean isTriggerComplete(Object sourceTarget, ConditionCfg conditionCfg, List<CheckerParam> checkParams) {
        return isTriggerComplete(sourceTarget, conditionCfg.getTriggerEventType(), checkParams);
    }


    /**
     * 是否触发成功
     *
     * @param conditionType 触发条件类型
     * @param checkParams   检查参数
     */
    public boolean isTriggerComplete(Object sourceTarget, String conditionType, List<CheckerParam> checkParams) {
        IConditionChecker conditionChecker = conditionCheckerMap.get(conditionType);
        if (conditionChecker instanceof IGameConditionChecker gameConditionChecker) {
            return gameConditionChecker.check(sourceTarget, checkParams);
        } else if (conditionChecker instanceof IPlayerConditionChecker playerConditionChecker) {
            if (sourceTarget instanceof Player player) {
                return playerConditionChecker.check(player, checkParams);
            }
            throw new IllegalArgumentException("调用系统条件触发方法，但是类型：" + conditionType + " 对应的检查类为玩家条件检查");
        }
        return false;
    }
}
