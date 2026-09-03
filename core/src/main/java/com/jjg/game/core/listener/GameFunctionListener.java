package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface GameFunctionListener extends IGameSysFuncInterface {
    /**
     * 通知功能开放
     * @param session
     * @param openedFuncIdList
     */
    default void notifyAllFunction(PFSession session, List<Integer> openedFuncIdList){}

    default Set<Integer> checkOpenFunction(Player player){
        return Collections.emptySet();
    }
}
