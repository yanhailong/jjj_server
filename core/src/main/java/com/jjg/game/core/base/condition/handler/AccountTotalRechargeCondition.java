package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.data.PlayerRecharge;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11005  账号个人累计充值 要求金额
 *
 * @author lm
 * @date 2026/1/14 10:35
 */
@Component
public class AccountTotalRechargeCondition implements ConditionHandler<PlayerRecharge> {

    private final CountDao countDao;

    public AccountTotalRechargeCondition(CountDao countDao) {
        this.countDao = countDao;
    }

    @Override
    public String type() {
        return "accountTotalRecharge";
    }

    @Override
    public PlayerRecharge parse(List<String> args) {
        String amount = args.getFirst();
        return new PlayerRecharge(0, 0, new BigDecimal(amount));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PlayerRecharge config) {
        BigDecimal count = countDao.getCount(CountDao.CountType.RECHARGE.getParam(), String.valueOf(ctx.player().getId()));
        if (count.compareTo(config.amount()) >= 0) {
            return MatchResultData.match();
        }
        return MatchResultData.notMatch(getErrorCode(), config.amount(), count);
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PlayerRecharge config) {
        return match(ctx, config);
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(11005);
        if (conditionCfg != null) {
            return conditionCfg.getLanguageID();
        }
        return 0;
    }
}

