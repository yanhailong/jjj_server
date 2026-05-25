package com.jjg.game.core.listener;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.ReqChooseSim;

/**
 * @author 11
 * @date 2026/5/25
 */
public interface ChooseSimListener {
    void onChooseSim(PlayerController playerController, ReqChooseSim req);
}
