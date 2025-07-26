package com.jjg.game.core.listener;

import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;

/**
 * @author 11
 * @date 2025/6/11 16:34
 */
public interface GmListener {
    CommonResult<String> gm(PlayerController playerController, String[] gmOrders);
}
