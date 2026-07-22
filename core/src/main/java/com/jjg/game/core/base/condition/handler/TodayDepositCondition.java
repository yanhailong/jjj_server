package com.jjg.game.core.base.condition.handler;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 11003：个人当日累计充值。Redis 只负责按自然日保存累计值。 */
@Component
public class TodayDepositCondition extends BaseRedisCondition<PreparedCondition> {
    private static final Logger log = LoggerFactory.getLogger(TodayDepositCondition.class);
    private final ConditionRuleRegistry conditionRules;

    protected TodayDepositCondition(CountDao countDao, ConditionRuleRegistry conditionRules) {
        super(countDao);
        this.conditionRules = conditionRules;
    }

    @Override
    public String type() {
        return "todayDeposit";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.RECHARGE;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(11003, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), dailyCustomId(ctx.player().getId()));
        return count.compareTo(BigDecimal.valueOf(config.target())) >= 0
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        if (!(ctx.event() instanceof PlayerRechargeEvent event) || event.getAmount() == null) {
            return match(ctx, config);
        }
        BigDecimal current = countDao.getCount(getFeatureId(ctx), dailyCustomId(ctx.player().getId()));
        if (current.compareTo(BigDecimal.valueOf(config.target())) >= 0) {
            return MatchResultData.match();
        }
        ConditionUpdate update = config.evaluate(LegacyConditionEventAdapter.recharge(event));
        if (!update.matched() || update.value() <= 0) {
            return MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), current);
        }
        BigDecimal total = countDao.incrementWithoutExpireRefresh(getFeatureId(ctx),
                dailyCustomId(ctx.player().getId()), event.getAmount(), TimeHelper.DAY_SECOND);
        return total.compareTo(BigDecimal.valueOf(config.target())) >= 0
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), BigDecimal.valueOf(config.target()), total);
    }

    @Override
    public void addBaseProgress(long playerId, BigDecimal addValue) {
        BigDecimal count = countDao.incrementWithoutExpireRefresh(type(), dailyCustomId(playerId),
                addValue, TimeHelper.DAY_SECOND);
        if (count.compareTo(BigDecimal.ZERO) == 0) {
            log.error("增加每日充值进度失败 playerId={} addValue={}", playerId, addValue.toPlainString());
        }
    }

    @Override
    public void delete(ConditionContext ctx, PreparedCondition config) {
        //自然日计数由 TTL 管理，不因单个活动删除而清空。
    }

    private static String dailyCustomId(long playerId) {
        return String.valueOf(playerId) + TimeHelper.getCurrentDateZeroSecondTime();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11003);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
