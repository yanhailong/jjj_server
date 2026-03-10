package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerRecharge;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11004个人累计充值（功能开启时计数）_总累计充值大于等于金额_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class TotalRechargeCondition extends BaseRedisCondition<PlayerRecharge> {


    protected TotalRechargeCondition(CountDao countDao) {
        super(countDao);
    }

    @Override
    public String type() {
        return "totalRecharge";
    }

    public boolean matchCheck(PlayerRechargeEvent event, PlayerRecharge config) {
        return (config.channelId() == 0 || event.getChannelId() == config.channelId());
    }

    @Override
    public PlayerRecharge parse(List<String> args) {
        String amount = args.getFirst();
        String channel = args.get(1);
        return new PlayerRecharge(0, Integer.parseInt(channel), new BigDecimal(amount));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerRecharge config) {
        BigDecimal count = countDao.getCount(getFeatureId(ctx), getCustomId(ctx));
        if (count.compareTo(config.amount()) >= 0) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.amount(), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerRecharge config) {
        if (ctx.event() instanceof PlayerRechargeEvent event && matchCheck(event, config)) {
            String customId = getCustomId(ctx);
            String featureId = getFeatureId(ctx);
            BigDecimal count = countDao.getCount(featureId, customId);
            if (count.compareTo(config.amount()) >= 0) {
                return MatchResultData.match();
            }
            BigDecimal add = countDao.incrBy(featureId, customId, event.getAmount());
            if (add.compareTo(config.amount()) >= 0) {
                return MatchResultData.match();
            }
            return MatchResultData.notMatch(getErrorCode(), config.amount(), count);
        }
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11004);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}

