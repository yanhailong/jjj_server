package com.jjg.game.core.base.condition.handler;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerRecharge;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11003 个人今日累计充值
 *
 * @author lm
 * @date 2026/1/14 13:48
 */
@Component
public class TodayDepositCondition extends BaseRedisCondition<PlayerRecharge> {


    private static final Logger log = LoggerFactory.getLogger(TodayDepositCondition.class);

    protected TodayDepositCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "todayDeposit";
    }

    @Override
    public PlayerRecharge parse(List<String> args) {
        String amount = args.getFirst();
        String channelId = args.get(1);
        return new PlayerRecharge(0, Integer.parseInt(channelId), new BigDecimal(amount));
    }

    public boolean matchCheck(PlayerRechargeEvent event, PlayerRecharge config) {
        return config.channelId() == 0 || event.getChannelId() == config.channelId();
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerRecharge config) {
        String customId = String.valueOf(ctx.player().getId()) + TimeHelper.getCurrentDateZeroSecondTime();
        BigDecimal count = countDao.getCount(getFeatureId(ctx) + ctx.player(), customId);
        if (count.compareTo(config.amount()) >= 0) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.amount(), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerRecharge config) {
        if (ctx.event() instanceof PlayerRechargeEvent event && matchCheck(event, config)) {
            String customId = String.valueOf(ctx.player().getId()) + TimeHelper.getCurrentDateZeroSecondTime();
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.compareTo(config.amount()) >= 0) {
                return MatchResultData.match();
            }
            count = countDao.incrementWithoutExpireRefresh(featureId, customId, event.getAmount(), TimeHelper.DAY_SECOND);
            if (count.compareTo(config.amount()) >= 0) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), config.amount(), count);
        }
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11003);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }

    @Override
    public void addBaseProgress(long playerId, BigDecimal addValue) {
        String customId = String.valueOf(playerId) + TimeHelper.getCurrentDateZeroSecondTime();
        String featureId = type();
        BigDecimal count = countDao.incrementWithoutExpireRefresh(featureId, customId, addValue, TimeHelper.DAY_SECOND);
        if (count.compareTo(BigDecimal.ZERO) == 0) {
            log.error("添加每日充值进度失败 playerId={} addValue={}", playerId, addValue.toPlainString());
        }
    }

    @Override
    public void delete(ConditionContext ctx, PlayerRecharge config) {
    }
}
