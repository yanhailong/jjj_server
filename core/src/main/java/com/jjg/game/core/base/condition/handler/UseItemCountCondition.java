package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.UserItemEvent;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 12101：累计使用指定道具。 */
@Component
public class UseItemCountCondition extends BaseRedisCondition<PreparedCondition> {
    private final ConditionRuleRegistry conditionRules;

    protected UseItemCountCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "useItemCount";
    }

    @Override
    public EGameEventType eventType() {
        return null;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(12101, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), customId(ctx, config));
        return count.longValue() >= config.target()
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), count.longValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof UserItemEvent event)) {
            return MatchResultData.unknown();
        }
        String customId = customId(ctx, config);
        BigDecimal current = countDao.getCount(getFeatureId(ctx), customId);
        if (current.longValue() >= config.target()) {
            return MatchResultData.match();
        }
        ConditionUpdate update = config.evaluate(new ActionConditionEvent(ActionConditionEvent.Type.ITEM_USE,
                event.getItemId(), 0, 0, event.getCount(), 0, false));
        if (!update.matched() || update.value() <= 0) {
            return MatchResultData.unknown();
        }
        BigDecimal total = countDao.incrBy(ctx.player().getId(), getFeatureId(ctx), customId,
                BigDecimal.valueOf(update.value()));
        return total.longValue() >= config.target()
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), total.longValue());
    }

    @Override
    public void delete(ConditionContext ctx, PreparedCondition config) {
        countDao.reset(ctx.player().getId(), getFeatureId(ctx), customId(ctx, config));
    }

    private static String customId(ConditionContext ctx, PreparedCondition config) {
        return String.valueOf(ctx.player().getId()) + config.spec().parameter(0);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(12101);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
