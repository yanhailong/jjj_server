package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerRecharge;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11001_单次充值大于等于金额_次数_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class SingleRechargeCondition extends BaseRedisCondition<PlayerRecharge> {
    protected SingleRechargeCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "singleRecharge";
    }

    public boolean matchCheck(PlayerRechargeEvent event, PlayerRecharge config) {
        return (config.channelId() == 0 || event.getChannelId() == config.channelId());

    }

    @Override
    public PlayerRecharge parse(List<String> args) {
        String amount = args.getFirst();
        String times = args.get(1);
        String channel = args.get(2);
        return new PlayerRecharge(Integer.parseInt(times), Integer.parseInt(channel), new BigDecimal(amount));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerRecharge config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (count.intValue() >= config.times()) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.times(), count.intValue());
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerRecharge config) {
        if (ctx.getEvent() instanceof PlayerRechargeEvent event && matchCheck(event, config)) {
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.intValue() >= config.times()) {
                return MatchResultData.match();
            }
            if (event.getAmount().compareTo(config.amount()) >= 0) {
                BigDecimal add = countDao.incrBy(featureId, customId, BigDecimal.ONE);
                if (add.intValue() >= config.times()) {
                    return MatchResultData.match();
                }
            }
            return MatchResultData.notMatch(getErrorCode(), config.times(), count.intValue());
        }
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11001);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}

