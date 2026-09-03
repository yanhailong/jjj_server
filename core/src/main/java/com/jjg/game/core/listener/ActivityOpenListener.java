package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;

/**
 * 查询活动类型是否存在正在运行的活动。
 */
public interface ActivityOpenListener extends IGameSysFuncInterface {
    boolean isActivityOpen(int activityType);
}
