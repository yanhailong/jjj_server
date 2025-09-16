package com.jjg.game.core.base.condition.checker;

import com.jjg.game.core.base.condition.IPlayerConditionChecker;
import com.jjg.game.core.base.drop.ConditionProgressDao;
import com.jjg.game.core.data.Player;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 基础条件进度类检查，带数据保存的
 *
 * @author 2CL
 */
public abstract class AbstractProgressConditionChecker implements IPlayerConditionChecker {
    // 条件进度查询dao
    @Autowired
    protected ConditionProgressDao conditionProgressDao;

    protected String getProgressKey(Player player) {
        return player.getId() + ":" + bindConditionCheckType();
    }
}
