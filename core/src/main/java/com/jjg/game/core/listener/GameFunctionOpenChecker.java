package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Player;

/**
 * 游戏功能额外开放条件。
 */
public interface GameFunctionOpenChecker extends IGameSysFuncInterface {
    default boolean isFunctionOpen(Player player, int functionId){
        return false;
    }
}
