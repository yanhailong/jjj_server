package com.jjg.game.core.constant;

/**
 * @author lm
 * @date 2025/7/15 16:33
 */
public interface BackendGMCmd {
    String CHANGE_GAME_STATUS = "changeGameStatus";

    interface Result {
        String SUCCESS = "success";
        String FAIL = "fail";
    }
}
