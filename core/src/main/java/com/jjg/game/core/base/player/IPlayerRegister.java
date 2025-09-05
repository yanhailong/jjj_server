package com.jjg.game.core.base.player;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Player;

/**
 * 玩家注册类
 *
 * @author 2CL
 */
public interface IPlayerRegister extends IGameSysFuncInterface {

    /**
     * 玩家注册
     */
    void playerRegister(Player player);
}
