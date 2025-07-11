package com.jjg.game.hall.constant;

import com.jjg.game.core.constant.Code;

/**
 * @author 11
 * @date 2025/6/10 17:15
 */
public interface HallCode extends Code {
    /**
     * 游戏状态:开启
     */
    int GAME_STATUS_OPEN = 0;
    /**
     * 游戏状态:维护
     */
    int GAME_STATUS_MAINTENANCE= 1;
    /**
     * 游戏状态:关闭
     */
    int GAME_STATUS_CLOSE = 2;

}
