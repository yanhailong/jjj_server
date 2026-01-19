package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.dao.CountDao;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/1/15 10:55
 */
public abstract class BaseRedisCondition<C> implements ConditionHandler<C> {
    protected final CountDao countDao;

    protected BaseRedisCondition(CountDao countDao) {
        this.countDao = countDao;
    }

    @Override
    public void delete(ConditionContext ctx, C config) {
        countDao.reset(getFeatureId(ctx), getCustomId(ctx));
    }

    public String getCustomId(ConditionContext ctx) {
        return String.valueOf(ctx.getPlayer().getId());
    }

    public String getFeatureId(ConditionContext ctx) {
        return type() + ctx.getPrefix();
    }

    public void addBaseProgress(long playerId, BigDecimal addValue){

    }
}
