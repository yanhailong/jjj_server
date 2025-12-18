package com.jjg.game.common.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;

/**
 * 服务器自动退出时调用
 * @author lm
 * @date 2025/11/7 14:05
 */
public interface OnServerAutoShutDown extends IGameSysFuncInterface {
    /**
     * 是否能退出
     * @return true 能 false不能
     */
    boolean canExit();
}
