package com.jjg.game.common.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.common.protostuff.PFSession;

/**
 * @author lm
 * @date 2025/11/28 09:31
 */
public interface OnSwitchNode extends IGameSysFuncInterface {
    void OnSwitchNodeAction(PFSession pfSession);
}
