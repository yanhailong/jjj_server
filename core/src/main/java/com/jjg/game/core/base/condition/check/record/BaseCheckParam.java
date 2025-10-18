package com.jjg.game.core.base.condition.check.record;

/**
 * @author lm
 * @date 2025/10/17 10:56
 */
public class BaseCheckParam {
    private String function;
    private long playerId;

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
}
