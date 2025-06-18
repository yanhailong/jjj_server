package com.jjg.game.core.listener;

import com.jjg.game.common.protostuff.PFSession;

/**
 * @author 11
 * @date 2025/6/11 16:34
 */
public interface GmListener {
    String gm(PFSession session,String cmd,String params);
}
