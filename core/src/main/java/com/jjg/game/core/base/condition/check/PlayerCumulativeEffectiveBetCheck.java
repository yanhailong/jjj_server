package com.jjg.game.core.base.condition.check;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.base.condition.check.record.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 12007_总累计达到设定有效下注数量（不计算开房间游戏）
 *
 * @author lm
 * @date 2025/10/17 10:11
 */
public class PlayerCumulativeEffectiveBetCheck extends BaseCheck {

    @Override
    public BigDecimal addProgress(Object paramObject, Object conditionObject) {
        if (paramObject instanceof PlayerSampleParam param && conditionObject instanceof PlayerSampleCondition condition) {
            BigDecimal progress = countDao.incrBy(param.getFunction(), getCustomId(param.getPlayerId()), BigDecimal.valueOf(param.getParamList().getFirst()));
            return progress.compareTo(condition.getMinAchievedValue()) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BaseCheckCondition analysisCondition(List<String> condition) {
        if (condition.isEmpty()) {
            return null;
        }
        PlayerSampleCondition sampleCondition = new PlayerSampleCondition();
        sampleCondition.setMinAchievedValue(new BigDecimal(condition.getFirst()).setScale(2, RoundingMode.DOWN));
        return sampleCondition;
    }

    @Override
    public Achieved getAchievedType() {
        return Achieved.PROGRESS;
    }
}
