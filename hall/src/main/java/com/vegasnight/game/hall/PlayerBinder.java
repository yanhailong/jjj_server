package com.vegasnight.game.hall;

import com.vegasnight.game.common.listener.SessionRefenerceBinder;
import com.vegasnight.game.common.protostuff.PFSession;
import com.vegasnight.game.core.data.Player;
import com.vegasnight.game.core.data.PlayerController;
import com.vegasnight.game.hall.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 9:31
 */
@Component
public class PlayerBinder implements SessionRefenerceBinder {
    @Autowired
    private PlayerService playerService;

    @Override
    public Object bind(PFSession session, long playerId) {
        if(playerId < 1){
            return null;
        }
        Player player = playerService.get(playerId);
        PlayerController playerController = new PlayerController(session, player);
        session.setReference(playerController);
        return playerController;
    }
}
