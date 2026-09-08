package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Player;

/**
 * 查询活动类型的全局运行状态和玩家参与资格。
 */
public interface ActivityOpenListener extends IGameSysFuncInterface {
    default boolean isActivityOpen(int activityType){
        return false;
    }

    /**
     * 对应类型存在运行中且该玩家可参与的活动时返回 true。
     */
    default boolean isActivityOpen(Player player, int activityType){
        return true;
    }
}
