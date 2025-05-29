package com.vegasnight.game.common.listener;

import com.vegasnight.game.common.protostuff.PFSession;

/**
 * @author 11
 * @date 2022/6/21
 */
public interface SessionLoginListener {
    void login(PFSession session, byte[] data);
}
