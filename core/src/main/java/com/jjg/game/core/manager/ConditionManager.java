package com.jjg.game.core.manager;

import com.jjg.game.core.base.condition.ConditionEngine;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.data.Player;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/10/17 10:46
 */
@Component
public class ConditionManager {

    private final ConditionEngine conditionEngine;

    public ConditionManager(ConditionEngine conditionEngine) {
        this.conditionEngine = conditionEngine;
    }

    /**
     * 添加进度并获取达成次数
     *
     * @param player    玩家
     * @param param     添加参数
     * @param condition 条件
     * @return 达成次数
     */
    public MatchResultData addProgressAndGetAchievements(Player player, Object param, String prefix, String condition) {
        return conditionEngine.addProgressAndCheck(player, param, prefix, condition);
    }


    /**
     * 是否达成条件
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievement(Player player, String prefix, String condition) {
        return conditionEngine.check(player, prefix, condition);
    }

    /**
     * 是否达成条件返回错误码
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public int isAchievementAndGetCode(Player player, String prefix, String condition) {
        return conditionEngine.checkAndGetCode(player, prefix, condition).errorCode();
    }

    /**
     * 是否达成条件返回错误码
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public MatchResultData isAchievementAndGetResult(Player player, String prefix, String condition) {
        return conditionEngine.checkAndGetCode(player, prefix, condition);
    }

    /**
     * 清除条件
     *
     * @param player    玩家
     * @param condition 条件参数
     */
    public void delete(Player player, String prefix, String condition) {
        conditionEngine.delete(player, prefix, condition);
    }


}
