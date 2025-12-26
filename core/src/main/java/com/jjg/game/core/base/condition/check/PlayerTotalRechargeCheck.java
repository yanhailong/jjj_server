package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.check.record.PlayerRechargeCondition;
import com.jjg.game.core.base.condition.check.record.PlayerRechargeParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 11002_总累计充值大于等于金额_渠道ID(0=默认所有)
 *
 * @author lm
 * @date 2025/10/17 09:51
 */
public class PlayerTotalRechargeCheck extends BaseCheck {

    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerRechargeParam param && conditionObject instanceof PlayerRechargeCondition condition) {

            if (param.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            //渠道检查
            BigDecimal progress = BigDecimal.ZERO;
            if (condition.getChannelId() == 0 || condition.getChannelId() == param.getChannelId()) {
                progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), param.getAmount());
            }
            return progress.compareTo(condition.getMinAchievedValue()) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public PlayerRechargeCondition analysisCondition(List<String> condition) {
        if (condition.size() < 2) {
            return null;
        }
        PlayerRechargeCondition playerRechargeCondition = new PlayerRechargeCondition();
        playerRechargeCondition.setMinAchievedValue(new BigDecimal(condition.getFirst()).setScale(2, RoundingMode.DOWN));
        playerRechargeCondition.setChannelId(Integer.parseInt(condition.get(1)));
        return playerRechargeCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
