package com.jjg.game.core.base.player;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;

/**
 * 登录成功监听
 *
 * @author lm
 * @date 2025/9/1 09:54
 */
public interface IPlayerLoginSuccess extends IGameSysFuncInterface {

    /**
     * 玩家登录成功事件
     *
     * @param playerController 玩家信息
     * @return true 继续执行 false终止执行
     */
    void onPlayerLoginSuccess(PlayerController playerController, Player player);
}
