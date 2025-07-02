package com.jjg.game.gate;

import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.listener.SessionVerifyListener;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/23 16:53
 */
@Component
public class UserEventController implements SessionVerifyListener {
    @Override
    public void userVerifyPass(String sessionId, long playerId, String ip) {
        GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
        if (gateSession != null) {
            gateSession.setCertify(true);
            gateSession.setPlayerId(playerId);
            gateSession.setHost(ip);
        }
    }
}
