package com.jjg.game.core.manager;

import com.jjg.game.core.base.condition.ConditionEngine;
import com.jjg.game.core.base.condition.MatchResult;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.utils.TipUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        return isAchievement(player, prefix, null, condition);
    }

    /**
     * 是否达成条件
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievement(Player player, String prefix, Object event, String condition) {
        return conditionEngine.check(player, prefix, event, condition);
    }

    /**
     * 是否达成条件并通知前端错误信息
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievementAndNotify(Player player, String prefix, String condition) {
        return isAchievementAndNotify(player, prefix, null, condition);
    }

    /**
     * 是否达成条件并通知前端错误信息
     *
     * @param player    玩家
     * @param event     触发事件
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public boolean isAchievementAndNotify(Player player, String prefix, Object event, String condition) {
        MatchResultData resultData = conditionEngine.checkAndGetCode(player, prefix, event, condition);
        if (resultData.result() == MatchResult.MATCH) {
            return true;
        }
        if (resultData.errorCode() > 0 && resultData.errorCode() != Code.SUCCESS) {
            Map<Integer, String> param = Map.of();
            if (resultData.need() != null) {
                param = Map.of(2, resultData.need().toPlainString());
            }
            TipUtils.sendToastTip(player.getId(), resultData.errorCode(), param);
        }
        return false;
    }

    /**
     * 是否达成条件返回错误码
     *
     * @param player    玩家
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public MatchResultData isAchievementAndGetResult(Player player, String prefix, String condition) {
        return conditionEngine.checkAndGetCode(player, prefix, null, condition);
    }

    /**
     * 是否达成条件返回错误码
     *
     * @param player    玩家
     * @param event     触发事件
     * @param condition 条件参数
     * @return 是否达成条件
     */
    public MatchResultData isAchievementAndGetResult(Player player, String prefix, Object event, String condition) {
        return conditionEngine.checkAndGetCode(player, prefix, event, condition);
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
