package com.jjg.game.hall.service;

import com.jjg.game.common.listener.SessionRefenerceBinder;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 9:31
 */
@Component
public class PlayerBinder implements SessionRefenerceBinder {
    @Autowired
    private HallPlayerService hallPlayerService;

    @Override
    public Object bind(PFSession session, long playerId) {
        if(playerId < 1){
            return null;
        }
        Player player = hallPlayerService.get(playerId);
        PlayerController playerController = new PlayerController(session, player);
        session.setReference(playerController);
        return playerController;
    }
}
