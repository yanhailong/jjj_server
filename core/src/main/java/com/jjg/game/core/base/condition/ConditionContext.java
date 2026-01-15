package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:29
 */

import com.jjg.game.core.data.Player;
import org.apache.poi.ss.formula.functions.T;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;

public class ConditionContext {

    private final Player player;
    private final Object event;
    private final String prefix;

    public ConditionContext(Player player, Object event, String prefix) {
        this.player = player;
        this.event = event;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getEvent() {
        return event;
    }
}
