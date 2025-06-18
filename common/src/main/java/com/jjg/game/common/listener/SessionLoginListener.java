package com.jjg.game.common.listener;

import com.jjg.game.common.protostuff.PFSession;

/**
 * @author 11
 * @date 2022/6/21
 */
public interface SessionLoginListener {
    void login(PFSession session, byte[] data);
}
