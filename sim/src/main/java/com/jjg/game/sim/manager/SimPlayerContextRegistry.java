package com.jjg.game.sim.manager;

import com.jjg.game.sim.data.SimPlayerContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 11
 * @date 2026/7/2
 */
@Component
public class SimPlayerContextRegistry {
    //储存玩家会话上下文
    private Map<Long, SimPlayerContext> contextMap = new ConcurrentHashMap<>();

    public SimPlayerContext getContext(long playerId) {
        return this.contextMap.get(playerId);
    }

    public SimPlayerContext putContext(SimPlayerContext ctx) {
        this.contextMap.put(ctx.playerId(), ctx);
        return ctx;
    }

    public SimPlayerContext removeContext(long playerId) {
        return this.contextMap.remove(playerId);
    }

    public int ctxSize() {
        return this.contextMap.size();
    }

    public Map<Long, SimPlayerContext> getContextMap() {
        return contextMap;
    }
}
