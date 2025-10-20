package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.check.record.PlayerRechargeCondition;
import com.jjg.game.core.base.condition.check.record.PlayerRechargeParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 11001_单次充值大于等于金额_次数_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2025/10/17 09:51
 */
public class PlayerRechargeCheck extends BaseCheck {

    @Override
    public long addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerRechargeParam param && conditionObject instanceof PlayerRechargeCondition condition) {

            if (param.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }
            //渠道检查
            long progress = 0;
            if (condition.getChannelId() == 0 || condition.getChannelId() == param.getChannelId()) {
                progress = getProgress(param);
                if (param.getAmount().compareTo(condition.getNeedAmount()) >= 0) {
                    progress++;
                    countDao.incr(param.getFunction(), getCustomId(param.getPlayerId()));
                }
            }
            return (int) (progress / condition.getAchievedTimes());
        }
        return 0;
    }

    @Override
    public PlayerRechargeCondition analysisCondition(List<Integer> condition) {
        if (condition.size() < 3) {
            return null;
        }
        PlayerRechargeCondition rechargeCondition = new PlayerRechargeCondition();
        rechargeCondition.setNeedAmount(BigDecimal.valueOf(condition.getFirst()));
        rechargeCondition.setAchievedTimes(condition.get(1));
        rechargeCondition.setChannelId(condition.get(2));
        return rechargeCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.TIMES;
    }
}
