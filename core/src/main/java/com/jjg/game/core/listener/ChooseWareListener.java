package com.jjg.game.core.listener;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.ReqChooseWare;

public interface ChooseWareListener {
    void onChooseWare(PlayerController playerController, ReqChooseWare req);
}
