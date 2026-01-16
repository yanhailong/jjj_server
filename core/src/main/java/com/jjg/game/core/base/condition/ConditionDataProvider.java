package com.jjg.game.core.base.condition;

/**
 * @author lm
 * @date 2026/1/14 10:29
 */

import java.math.BigDecimal;
import java.util.Optional;

public interface ConditionDataProvider {

    boolean supports(String key);

    Optional<Object> get(ConditionContext ctx, String key);

    default Optional<Object> update(ConditionContext ctx, String key, long playerId, BigDecimal delta) {
        return Optional.empty();
    }

}
