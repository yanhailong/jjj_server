package com.jjg.game.core.listener;

import com.jjg.game.core.data.PlayerController;

/**
 * @author 11
 * @date 2025/6/11 16:34
 */
public interface GmListener {
    String gm(PlayerController playerController, String cmd, String params);
}
