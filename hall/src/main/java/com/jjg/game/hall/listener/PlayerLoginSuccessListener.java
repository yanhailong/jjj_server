package com.jjg.game.hall.listener;

import com.jjg.game.core.data.PlayerController;

/**
 * 登录成功监听
 *
 * @author lm
 * @date 2025/9/1 09:54
 */
public interface PlayerLoginSuccessListener {
    /**
     * 玩家登录成功事件
     * @param playerController 玩家信息
     * @return true 继续执行 false终止执行
     */
    boolean onPlayerLoginSuccess(PlayerController playerController);

    default int getOrder() {
        return 1;
    }
}
