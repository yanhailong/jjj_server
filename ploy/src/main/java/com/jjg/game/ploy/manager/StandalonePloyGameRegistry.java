package com.jjg.game.ploy.manager;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StandalonePloyGameRegistry {
    private final Map<Integer, StandalonePloyGame> games = new HashMap<>();

    public StandalonePloyGameRegistry(List<StandalonePloyGame> standaloneGames) {
        for (StandalonePloyGame game : standaloneGames) {
            StandalonePloyGame previous = games.putIfAbsent(game.gameType(), game);
            if (previous != null) {
                throw new IllegalStateException("Duplicate standalone Ploy game type: " + game.gameType());
            }
        }
    }

    public StandalonePloyGame get(int gameType) {
        return games.get(gameType);
    }
}
