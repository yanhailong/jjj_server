package com.vegasnight.game.gate;

import com.vegasnight.game.common.gate.GateSession;
import com.vegasnight.game.common.listener.SessionVerifyListener;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/23 16:53
 */
@Component
public class UserEventController implements SessionVerifyListener {
    @Override
    public void userVerifyPass(String sessionId, long playerId, String ip) {
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.certify = true;
            gateSession.playerId = playerId;
            gateSession.setHost(ip);
        }
    }
}
