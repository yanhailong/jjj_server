package com.jjg.game.core.listener;

import com.jjg.game.common.protostuff.PFSession;

import java.util.List;

public interface GameFunctionListener {
    /**
     * 通知功能开放
     * @param session
     * @param openedFuncIdList
     */
    void notifyAllFunction(PFSession session, List<Integer> openedFuncIdList);
}
