package com.jjg.game.core.base.condition.data;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/1/14 15:18
 */
public record PlayerRecharge(
        int times,
        int channelId,
        BigDecimal amount) {
}
