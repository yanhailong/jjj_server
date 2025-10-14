package com.jjg.game.core.data;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2025/10/13 17:44
 */
public record ExperienceBonusParam(BigDecimal expProp, BigDecimal statementProp, BigDecimal value, long addExp) {
}