package com.jjg.game.slots.service;

import com.jjg.game.common.listener.SessionReferenceBinder;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/11/20 9:46
 */
@Component
public class SlotsPlayerBinder implements SessionReferenceBinder {
    @Autowired
    private SlotsPlayerService playerService;

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
