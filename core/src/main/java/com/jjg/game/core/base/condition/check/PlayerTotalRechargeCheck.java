package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.check.record.PlayerRechargeCondition;
import com.jjg.game.core.base.condition.check.record.PlayerRechargeParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11002_总累计充值大于等于金额_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2025/10/17 09:51
 */
public class PlayerTotalRechargeCheck extends BaseCheck {

    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerRechargeParam param && conditionObject instanceof PlayerRechargeCondition condition) {

            if (param.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
            //渠道检查
            long progress = 0;
            if (condition.getChannelId() == 0 || condition.getChannelId() == param.getChannelId()) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), param.getAmount().longValue());
            }
            return progress >= condition.getNeedAmount().longValue() ? 1 : 0;
        }
        return 0;
    }

    @Override
    public PlayerRechargeCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 2) {
            return null;
        }
        PlayerRechargeCondition playerRechargeCondition = new PlayerRechargeCondition();
        playerRechargeCondition.setNeedAmount(BigDecimal.valueOf(condition.getFirst()));
        playerRechargeCondition.setChannelId(condition.get(1));
        return playerRechargeCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
